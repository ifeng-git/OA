package com.oa.jbpm.handler;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oa.util.Constant;

public class RollbackTransitionAction implements ActionHandler {

	private Logger logger = LoggerFactory.getLogger(RollbackTransitionAction.class);

	TaskMgmtInstance getTaskMgmtInstance(Token token) {
		return (TaskMgmtInstance) token.getProcessInstance().getInstance(TaskMgmtInstance.class);
	}

	public void execute(ExecutionContext executionContext) throws Exception {
		Token rootToken = executionContext.getProcessInstance().getRootToken();
		Token currentToken = executionContext.getToken();
		logger.debug("����Action");
		String forkName = (String) executionContext.getContextInstance().getVariable("forkName" + currentToken.getId());
		String preNodeName = (String) executionContext.getContextInstance().getVariable("preNodeName" + currentToken.getId());
		Session session = executionContext.getJbpmContext().getSession();
		TaskNode taskNode = (TaskNode) executionContext.getNode();
//		executionContext.getProcessInstance().getRootToken().getId()
		Set transitionSet=currentToken.getAvailableTransitions();
		for (Iterator iterator = transitionSet.iterator(); iterator.hasNext();) {
			Transition t = (Transition) iterator.next();
			Node fromNode = t.getTo();
			String forkname = fromNode.toString();
			if (forkname.matches(Constant.forkRegex)) {
//				executionContext.getProcessInstance().getContextInstance().getVariable("currentNode");
//				executionContext.getProcessInstance().getContextInstance().setVariable("currentNode", taskNode.getName());
				JbpmUtil.rollbackFork(currentToken.getId(), executionContext.getProcessInstance().getId(), taskNode.getName(), session,taskNode.getId());
			}
		}
		

		boolean isForkOrJoin = false;
		logger.info("���̶���ڵ�Token" + rootToken == null ? "null" : rootToken + "==");
		logger.info("��ǰ�ڵ�Token" + currentToken);
		logger.info("��ǰ�ڵ㸸��Token" + currentToken.getParent());
		logger.info(executionContext.toString());

		// �����ǰ�ڵ����ڽ����ڵ㣬�����贴���κη���Transition
		if (executionContext.getNode() instanceof EndState) {
			return;
		}

		// ����ýڵ��transition
		Set<Transition> arrayingTransition = taskNode.getArrivingTransitions();
		// �ýڵ�Ҫ�뿪��·��
		List<Transition> leavingTransitions = taskNode.getLeavingTransitions();

		// ��ֹ���������Ϊһ��֩����
		if (arrayingTransition.size() < 2) {
			boolean ignore = false;
			boolean isReturn = false;

			// �õ���ǰָ��Ľڵ����п���ʹ�õ�transition�����б�
			Set ts = executionContext.getToken().getAvailableTransitions();

			for (Iterator iterator = ts.iterator(); iterator.hasNext();) {
				Transition t = (Transition) iterator.next();
				String transitionName = t.getName();
				if (transitionName.matches(Constant.rollbackRegex)) {
					ignore = true;
					executionContext.getContextInstance().setVariable("forkName" + currentToken.getId(), taskNode.getName());
					// break;
				}
				Node fromNode = t.getTo();
				String forkname = fromNode.toString();
				logger.info("��ǰ�ڵ�����" + fromNode.getName());
				logger.info("��ǰ�ڵ�ָ����һ���ڵ�Ϊ" + t.getTo().getName());

				if (forkname.matches(Constant.forkRegex)) {
					List<Transition> forkTransitions = fromNode.getLeavingTransitions();
					// forkTransitions.get(0).getTo()
					for (Transition tra : forkTransitions) {
						// tra.getFrom();
						Node childNameNode = tra.getTo();

						Transition transition = new Transition();
						transition.setName(Constant.rollback + executionContext.getNode().getName());

						childNameNode.addLeavingTransition(transition);
						executionContext.getNode().addArrivingTransition(transition);
					}
					executionContext.getContextInstance().setVariable("preNodeName" + currentToken.getId(), executionContext.getNode().getName());
					isReturn = true;
				}

			}
			if (isReturn)
				return;

			// �����δ��������Transition���󣬾Ϳ��Լ�������
			if (!ignore) {
				if (rootToken.getId() != currentToken.getId()) {
					// isForkOrJoin = true;
					String nodeName = (String) executionContext.getContextInstance().getVariable("forkName" + currentToken.getId());
					// String nodeName = (String)
					// executionContext.getContextInstance().getVariable(temp1);
					preNodeName = nodeName;
				}
				if (preNodeName != null) { // ǰһ���ڵ�ǿգ���Ҫ��������Transition
					// �ӵ�ǰ�ڵ�
					Node from = executionContext.getNode();

					Node to = null;

					to = executionContext.getProcessDefinition().getNode(preNodeName);

					// ��������Transition����
					Transition transition = new Transition();
					transition.setName(Constant.rollback + preNodeName);

					from.addLeavingTransition(transition);
					to.addArrivingTransition(transition);
				}
				if (rootToken.getId() == currentToken.getId()) {
					executionContext.getContextInstance().setVariable("preNodeName" + currentToken.getId(), executionContext.getNode().getName());

				} else {
					executionContext.getContextInstance().setVariable("forkName" + currentToken.getId(), taskNode.getName());
				}
			}

		}

	}
}
