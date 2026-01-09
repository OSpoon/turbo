package com.didiglobal.turbo.engine.processor;

import com.didiglobal.turbo.engine.common.FlowElementType;
import com.didiglobal.turbo.engine.dao.mapper.FlowDeploymentMapper;
import com.didiglobal.turbo.engine.entity.FlowDeploymentPO;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.didiglobal.turbo.engine.param.CommitTaskParam;
import com.didiglobal.turbo.engine.param.StartProcessParam;
import com.didiglobal.turbo.engine.result.CommitTaskResult;
import com.didiglobal.turbo.engine.result.NodeInstanceListResult;
import com.didiglobal.turbo.engine.result.NodeInstanceResult;
import com.didiglobal.turbo.engine.result.StartProcessResult;
import com.didiglobal.turbo.engine.runner.TestEngineApplication;
import com.didiglobal.turbo.engine.util.EntityBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = TestEngineApplication.class)
public class RuntimeProcessorFixTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeProcessorFixTest.class);

    @Resource
    private RuntimeProcessor runtimeProcessor;

    @Resource
    private FlowDeploymentMapper flowDeploymentMapper;

    private StartProcessResult startProcess() throws Exception {
        // prepare
        FlowDeploymentPO flowDeploymentPO = EntityBuilder.buildSpecialFlowDeploymentPO();
        FlowDeploymentPO _flowDeploymentPO = flowDeploymentMapper.selectByDeployId(flowDeploymentPO.getFlowDeployId());
        if (_flowDeploymentPO != null) {
            if (!StringUtils.equals(_flowDeploymentPO.getFlowModel(), flowDeploymentPO.getFlowModel())) {
                flowDeploymentMapper.deleteById(_flowDeploymentPO.getId());
                flowDeploymentMapper.insert(flowDeploymentPO);
            }
        } else {
            flowDeploymentMapper.insert(flowDeploymentPO);
        }

        // start process
        StartProcessParam startProcessParam = new StartProcessParam();
        startProcessParam.setFlowDeployId(flowDeploymentPO.getFlowDeployId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("orderId", "123"));
        variables.add(new InstanceData("orderStatus", "1"));
        startProcessParam.setVariables(variables);
        // build
        return runtimeProcessor.startProcess(startProcessParam);
    }

    @Test
    public void testGetHistoryUserTaskListWithFlowElementType() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        CommitTaskParam commitTaskParam = new CommitTaskParam();
        commitTaskParam.setFlowInstanceId(startProcessResult.getFlowInstanceId());
        commitTaskParam.setTaskInstanceId(startProcessResult.getActiveTaskInstance().getNodeInstanceId());
        List<InstanceData> variables = new ArrayList<>();
        variables.add(new InstanceData("danxuankuang_ytgyk", 0));
        commitTaskParam.setVariables(variables);

        // UserTask -> ExclusiveGateway -> UserTask
        CommitTaskResult commitTaskResult = runtimeProcessor.commit(commitTaskParam);

        NodeInstanceListResult nodeInstanceListResult = runtimeProcessor.getHistoryUserTaskList(commitTaskResult.getFlowInstanceId(), false);
        LOGGER.info("testGetHistoryUserTaskList.||nodeInstanceListResult={}", nodeInstanceListResult);
        
        Assertions.assertTrue(nodeInstanceListResult.getNodeInstanceList().size() == 2);
        Assertions.assertTrue(StringUtils.equals(nodeInstanceListResult.getNodeInstanceList().get(0).getModelKey(), "UserTask_0uld0u9"));
        // This is the bug fix verification
        Assertions.assertEquals(FlowElementType.USER_TASK, nodeInstanceListResult.getNodeInstanceList().get(0).getFlowElementType(), "FlowElementType should be USER_TASK(4)");
    }

    @Test
    public void testGetNodeInstanceWithFlowElementType() throws Exception {
        StartProcessResult startProcessResult = startProcess();
        String flowInstanceId = startProcessResult.getFlowInstanceId();
        NodeInstanceResult nodeInstanceResult = runtimeProcessor.getNodeInstance(flowInstanceId, startProcessResult.getActiveTaskInstance().getNodeInstanceId(), false);
        LOGGER.info("testGetNodeInstance.||nodeInstanceResult={}", nodeInstanceResult);

        Assertions.assertTrue(StringUtils.equals(nodeInstanceResult.getNodeInstance().getNodeInstanceId(), startProcessResult.getActiveTaskInstance().getNodeInstanceId()));
        // This is the bug fix verification
        Assertions.assertEquals(FlowElementType.USER_TASK, nodeInstanceResult.getNodeInstance().getFlowElementType(), "FlowElementType should be USER_TASK(4)");
    }
}