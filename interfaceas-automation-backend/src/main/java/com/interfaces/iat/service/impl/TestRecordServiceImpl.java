package com.interfaces.iat.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interfaces.iat.dto.common.ResponseData;
import com.interfaces.iat.dto.input.testrecord.TestRecordCreateInputDto;
import com.interfaces.iat.dto.input.testrecord.TestRecordUpdateInputDto;
import com.interfaces.iat.dto.output.task.TaskOutputDto;
import com.interfaces.iat.dto.output.testrecord.TestRecordOutputDto;
import com.interfaces.iat.dto.output.testrecord.TestRecordQueryOutputDto;
import com.interfaces.iat.dto.output.testreport.TestReportOutputDto;
import com.interfaces.iat.dto.output.testreport.TestResultOutputDto;
import com.interfaces.iat.entity.*;
import com.interfaces.iat.mapper.TestRecordMapper;
import com.interfaces.iat.service.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestRecordServiceImpl extends ServiceImpl<TestRecordMapper, TestRecordEntity> implements TestRecordService {
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    EnvironmentService environmentService;
    @Autowired
    TaskService taskService;
    @Autowired
    ProjectService projectService;
    @Autowired
    TestReportService testReportService;

    @Override
    public ResponseData<List<TestRecordQueryOutputDto>> query(Integer pageIndex, Integer pageSize, Integer environmentId, Integer taskId, Integer projectId) {
        ResponseData<List<TestRecordQueryOutputDto>> responseData;

        try {
            QueryWrapper<TestRecordEntity> queryWrapper = new QueryWrapper<>();
            if(environmentId != null) {
                queryWrapper.eq("environment_id", environmentId);
            }
            if(taskId != null) {
                queryWrapper.eq("task_id", taskId);
            }
            if(projectId != null) {
                queryWrapper.eq("project_id", projectId);
            }
            queryWrapper.eq("is_delete",false); //?????????????????????
            queryWrapper.orderByDesc("id");
            IPage<TestRecordEntity> queryPage = new Page<>(pageIndex, pageSize);
            queryPage = this.page(queryPage,queryWrapper);
            List<TestRecordQueryOutputDto> testRecordQueryOutputDtos = queryPage.getRecords().stream().map(s->modelMapper.map(s, TestRecordQueryOutputDto.class)).collect(Collectors.toList());

            //??????????????????
            List<Integer> taskIds = testRecordQueryOutputDtos.stream().map(s->s.getTaskId()).collect(Collectors.toList());
            QueryWrapper<TaskEntity> taskOutputDtoQueryWrapper = new QueryWrapper <>();
            taskOutputDtoQueryWrapper.in("id",taskIds);
            List<TaskEntity> taskEntities = taskService.list(taskOutputDtoQueryWrapper);


            //??????????????????
            List<Integer> testRecordIds = testRecordQueryOutputDtos.stream().map(s->s.getId()).collect(Collectors.toList());
            QueryWrapper<TestReportEntity> testReportEntityQueryWrapper = new QueryWrapper<>();
            testReportEntityQueryWrapper.in("test_record_id", testRecordIds);
            List<TestReportEntity> testReportEntities = testReportService.list(testReportEntityQueryWrapper);

            //??????????????????
            testRecordQueryOutputDtos.stream().forEach(s->{
                //????????????
                TaskEntity taskEntity = taskEntities.stream().filter(t->t.getId() == s.getTaskId()).findFirst().orElse(null);
                if(taskEntity!=null){
                    s.setTask(modelMapper.map(taskEntity,TaskOutputDto.class));
                }
                //????????????
                TestReportEntity testReportEntity = testReportEntities.stream().filter(t->t.getTestRecordId().intValue() ==s.getId().intValue()).findFirst().orElse(null);
                if(testReportEntity!=null){
                    TestReportOutputDto testReportOutputDto = modelMapper.map(testReportEntity, TestReportOutputDto.class);
                    //???????????????????????????Result??????????????????????????????????????????????????????????????????????????????????????????
                    TestResultOutputDto testResultOutputDto = modelMapper.map(JSON.parse(testReportOutputDto.getResult()),TestResultOutputDto.class);
                    testReportOutputDto.setTotalOfTestCase(testResultOutputDto.getTotalOfTestCase());
                    testReportOutputDto.setTotalOfTestCaseForSuccess(testResultOutputDto.getTotalOfTestCaseForSuccess());
                    testReportOutputDto.setTotalOfTestCaseForFailure(testResultOutputDto.getTotalOfTestCaseForFailure());
                    testReportOutputDto.setTotalOfTestCaseForError(testResultOutputDto.getTotalOfTestCaseForError());
                    //????????????
                    testReportOutputDto.setResult(null);

                    s.setTestReport(testReportOutputDto);
                }
            });

            responseData = ResponseData.success(testRecordQueryOutputDtos);
            responseData.setTotal(queryPage.getTotal());
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData<List<TestRecordQueryOutputDto>> queryByProjectId(Integer taskId,Integer projectId) {
        ResponseData<List<TestRecordQueryOutputDto>> responseData;

        try {
            QueryWrapper<TestRecordEntity> queryWrapper = new QueryWrapper<>();
            if(taskId != null) {
                queryWrapper.eq("task_id", taskId);
            }
            if(projectId != null) {
                queryWrapper.eq("project_id", projectId);
            }
            queryWrapper.eq("is_delete",false);
            queryWrapper.eq("status",0);
            queryWrapper.orderByDesc("id");
            List <TestRecordEntity> entities = this.list(queryWrapper);
            List <TestRecordQueryOutputDto> outputDtos = entities.stream().map(s -> modelMapper.map(s, TestRecordQueryOutputDto.class)).collect(Collectors.toList());

            //??????????????????
            List<TestReportEntity> testReportEntities = new ArrayList<>();
            List<Integer> testRecordIds = outputDtos.stream().map(s->s.getId()).collect(Collectors.toList());
            if(testRecordIds!=null && testRecordIds.size()>0) {
                QueryWrapper<TestReportEntity> testReportEntityQueryWrapper = new QueryWrapper<>();
                testReportEntityQueryWrapper.in("test_record_id", testRecordIds);
                testReportEntities = testReportService.list(testReportEntityQueryWrapper);
            }

            List<TestReportEntity> finalTestReportEntities = testReportEntities;
            outputDtos.stream().forEach(s->{
                TestReportEntity testReportEntity = finalTestReportEntities.stream().filter(t->t.getTestRecordId().intValue() ==s.getId().intValue()).findFirst().orElse(null);
                if(testReportEntity!=null){
                    TestReportOutputDto testReportOutputDto = modelMapper.map(testReportEntity, TestReportOutputDto.class);
                    //???????????????????????????Result??????????????????????????????????????????????????????????????????????????????????????????
                    TestResultOutputDto testResultOutputDto = modelMapper.map(JSON.parse(testReportOutputDto.getResult()),TestResultOutputDto.class);
                    testReportOutputDto.setTotalOfTestCase(testResultOutputDto.getTotalOfTestCase());
                    testReportOutputDto.setTotalOfTestCaseForSuccess(testResultOutputDto.getTotalOfTestCaseForSuccess());
                    testReportOutputDto.setTotalOfTestCaseForFailure(testResultOutputDto.getTotalOfTestCaseForFailure());
                    testReportOutputDto.setTotalOfTestCaseForError(testResultOutputDto.getTotalOfTestCaseForError());
                    //????????????
                    testReportOutputDto.setResult(null);
                    s.setTestReport(testReportOutputDto);
                }
            });

            responseData = ResponseData.success(outputDtos);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData <TestRecordOutputDto> getById(Integer id) {
        ResponseData<TestRecordOutputDto> responseData;
        try {
            //??????????????????
            TestRecordEntity entity = super.getById(id);
            //????????????????????????
            QueryWrapper<TestReportEntity> testReportEntityQueryWrapper = new QueryWrapper<>();
            testReportEntityQueryWrapper.eq("test_record_id",entity.getId());
            TestReportEntity testReportEntity = testReportService.getOne(testReportEntityQueryWrapper,false);

            TestRecordOutputDto outputDto = modelMapper.map(entity,TestRecordOutputDto.class);
            if(testReportEntity!=null){
                TestReportOutputDto testReportOutputDto = modelMapper.map(testReportEntity,TestReportOutputDto.class);
                //??????????????????????????????????????????????????????
                TestResultOutputDto testResultOutputDto = modelMapper.map(JSON.parse(testReportOutputDto.getResult()),TestResultOutputDto.class);
                testReportOutputDto.setTotalOfTestCase(testResultOutputDto.getTotalOfTestCase());
                testReportOutputDto.setTotalOfTestCaseForSuccess(testResultOutputDto.getTotalOfTestCaseForSuccess());
                testReportOutputDto.setTotalOfTestCaseForFailure(testResultOutputDto.getTotalOfTestCaseForFailure());
                testReportOutputDto.setTotalOfTestCaseForError(testResultOutputDto.getTotalOfTestCaseForError());
                outputDto.setTestReport(testReportOutputDto);
            }

            responseData = ResponseData.success(outputDto);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData<TestRecordOutputDto> create(TestRecordCreateInputDto inputDto) {
        ResponseData<TestRecordOutputDto> responseData;

        try {
            List<String> checkMsgs = new ArrayList <>();
            //??????????????????????????????
            QueryWrapper<EnvironmentEntity> environmentEntityQueryWrapper = new QueryWrapper<>();
            environmentEntityQueryWrapper.eq("id", inputDto.getEnvironmentId());
            environmentEntityQueryWrapper.eq("is_delete", false);
            Integer existCount = environmentService.count(environmentEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("???????????????????????????");
            }
            //??????????????????????????????
            QueryWrapper<TaskEntity> taskEntityQueryWrapper = new QueryWrapper<>();
            taskEntityQueryWrapper.eq("id", inputDto.getTaskId());
            taskEntityQueryWrapper.eq("is_delete", false);
            existCount = taskService.count(taskEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("?????????????????????");
            }
            //????????????????????????
            QueryWrapper<ProjectEntity> projectEntityQueryWrapper = new QueryWrapper<>();
            projectEntityQueryWrapper.eq("id", inputDto.getEnvironmentId());
            projectEntityQueryWrapper.eq("is_delete", false);
            existCount = projectService.count(projectEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("?????????????????????");
            }
            if(checkMsgs.size()>0){
                responseData = new ResponseData <>();
                responseData.setCode(1);
                responseData.setMessage(checkMsgs.stream().collect(Collectors.joining(",")));

                return responseData;
            }

            TestRecordEntity entity = modelMapper.map(inputDto,TestRecordEntity.class);
            entity.setIsDelete(false);
            this.save(entity);
            TestRecordOutputDto outputDto = modelMapper.map(entity,TestRecordOutputDto.class);

            responseData = ResponseData.success(outputDto);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData<TestRecordOutputDto> update(TestRecordUpdateInputDto inputDto) {
        ResponseData<TestRecordOutputDto> responseData;

        try {
            List<String> checkMsgs = new ArrayList <>();
            //??????????????????????????????
            QueryWrapper<EnvironmentEntity> environmentEntityQueryWrapper = new QueryWrapper<>();
            environmentEntityQueryWrapper.eq("id", inputDto.getEnvironmentId());
            environmentEntityQueryWrapper.eq("is_delete", false);
            Integer existCount = environmentService.count(environmentEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("???????????????????????????");
            }
            //??????????????????????????????
            QueryWrapper<TaskEntity> taskEntityQueryWrapper = new QueryWrapper<>();
            taskEntityQueryWrapper.eq("id", inputDto.getTaskId());
            taskEntityQueryWrapper.eq("is_delete", false);
            existCount = taskService.count(taskEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("?????????????????????");
            }
            //????????????????????????
            QueryWrapper<ProjectEntity> projectEntityQueryWrapper = new QueryWrapper<>();
            projectEntityQueryWrapper.eq("id", inputDto.getEnvironmentId());
            projectEntityQueryWrapper.eq("is_delete", false);
            existCount = projectService.count(projectEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("?????????????????????");
            }
            if(checkMsgs.size()>0){
                responseData = new ResponseData <>();
                responseData.setCode(1);
                responseData.setMessage(checkMsgs.stream().collect(Collectors.joining(",")));

                return responseData;
            }

            TestRecordEntity entity = modelMapper.map(inputDto,TestRecordEntity.class);
            entity.setIsDelete(false);
            this.updateById(entity);
            TestRecordOutputDto outputDto = modelMapper.map(entity,TestRecordOutputDto.class);

            responseData = ResponseData.success(outputDto);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }
}
