package com.interfaces.iat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interfaces.iat.dto.common.ResponseData;
import com.interfaces.iat.dto.input.testcase.TestCaseCreateInputDto;
import com.interfaces.iat.dto.input.testcase.TestCaseUpdateInputDto;
import com.interfaces.iat.dto.output.testcase.TestCaseOutputDto;
import com.interfaces.iat.entity.InterfaceEntity;
import com.interfaces.iat.entity.TaskModuleEntity;
import com.interfaces.iat.entity.TestCaseEntity;
import com.interfaces.iat.entity.TestSuitEntity;
import com.interfaces.iat.mapper.TestCaseMapper;
import com.interfaces.iat.service.InterfaceService;
import com.interfaces.iat.service.TaskModuleService;
import com.interfaces.iat.service.TestCaseService;
import com.interfaces.iat.service.TestSuitService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestCaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCaseEntity> implements TestCaseService {
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    InterfaceService interfaceService;
    @Autowired
    TestSuitService testSuitService;
    @Autowired
    TaskModuleService taskModuleService;

    @Override
    public ResponseData<List<TestCaseOutputDto>> query(Integer pageIndex, Integer pageSize, Integer interfaceId, Integer testSuitId, Integer taskId, Integer projectId) {
        ResponseData<List<TestCaseOutputDto>> responseData;

        try {
            QueryWrapper<TestCaseEntity> queryWrapper = new QueryWrapper<>();
            if(interfaceId != null) {
                queryWrapper.eq("interface_id", interfaceId);
            }
            if(testSuitId != null) {
                queryWrapper.eq("test_suit_id", testSuitId);
            }
            if(taskId != null) {
                queryWrapper.eq("task_id", taskId);
            }
            if(projectId != null) {
                queryWrapper.eq("project_id", projectId);
            }
            queryWrapper.eq("is_delete",false); //?????????????????????
            queryWrapper.orderByDesc("id");
            IPage<TestCaseEntity> queryPage = new Page<>(pageIndex, pageSize);
            queryPage = this.page(queryPage,queryWrapper);
            List<TestCaseOutputDto> interfaceQueryOutputDtos = queryPage.getRecords().stream().map(s->modelMapper.map(s, TestCaseOutputDto.class)).collect(Collectors.toList());


            responseData = ResponseData.success(interfaceQueryOutputDtos);
            responseData.setTotal(queryPage.getTotal());
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData<List<TestCaseOutputDto>> queryByProjectId(Integer projectId) {
        ResponseData<List<TestCaseOutputDto>> responseData;

        try {
            QueryWrapper<TestCaseEntity> queryWrapper = new QueryWrapper<>();
            if(projectId != null) {
                queryWrapper.eq("project_id", projectId);
            }
            queryWrapper.eq("is_delete",false); //?????????????????????
            queryWrapper.orderByDesc("id");
            List <TestCaseEntity> entities = this.list(queryWrapper);
            List <TestCaseOutputDto> outputDtos = entities.stream().map(s -> modelMapper.map(s, TestCaseOutputDto.class)).collect(Collectors.toList());

            responseData = ResponseData.success(outputDtos);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData <TestCaseOutputDto> getById(Integer id) {
        ResponseData<TestCaseOutputDto> responseData;
        try {
            TestCaseEntity entity = super.getById(id);

            TestCaseOutputDto outputDto = modelMapper.map(entity,TestCaseOutputDto.class);

            responseData = ResponseData.success(outputDto);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData<TestCaseOutputDto> create(TestCaseCreateInputDto inputDto) {
        ResponseData<TestCaseOutputDto> responseData;

        try {
            List<String> checkMsgs = new ArrayList <>();
            //??????????????????????????????
            QueryWrapper<TestCaseEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", inputDto.getName());
            queryWrapper.eq("is_delete",false);
            queryWrapper.eq("interface_id",inputDto.getInterfaceId());
            queryWrapper.eq("test_suit_id",inputDto.getTestSuitId());
            TestCaseEntity testCaseEntity = this.getOne(queryWrapper,false);
            if (testCaseEntity!=null) {
                checkMsgs.add("??????????????????????????????");
            }
            //????????????????????????
            QueryWrapper<InterfaceEntity> interfaceEntityQueryWrapper = new QueryWrapper<>();
            interfaceEntityQueryWrapper.eq("id", inputDto.getInterfaceId());
            interfaceEntityQueryWrapper.eq("is_delete", false);
            Integer existCount = interfaceService.count(interfaceEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("?????????????????????");
            }
            //??????????????????????????????
            QueryWrapper<TestSuitEntity> testSuitEntityQueryWrapper = new QueryWrapper<>();
            testSuitEntityQueryWrapper.eq("id", inputDto.getTestSuitId());
            testSuitEntityQueryWrapper.eq("is_delete", false);
            existCount = testSuitService.count(testSuitEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("???????????????????????????");
            }
            //????????????????????????????????????
            QueryWrapper<TaskModuleEntity> taskModuleEntityQueryWrapper = new QueryWrapper<>();
            taskModuleEntityQueryWrapper.eq("task_id", inputDto.getTaskId());
            //??????????????????id
            List<TaskModuleEntity> taskModuleEntities = taskModuleService.list(taskModuleEntityQueryWrapper);
            //???????????????id?????????????????????id
            long ExistModuleIdCount = taskModuleEntities.stream().filter(f -> f.getModuleId().intValue() == inputDto.getModuleId().intValue()).count();
            if((taskModuleEntities==null && taskModuleEntities.size()<=0)||ExistModuleIdCount==0){
                checkMsgs.add("???????????????????????????,???????????????");
            }


            if(checkMsgs.size()>0){
                responseData = new ResponseData <>();
                responseData.setCode(1);
                responseData.setMessage(checkMsgs.stream().collect(Collectors.joining(",")));

                return responseData;
            }

            TestCaseEntity entity = modelMapper.map(inputDto,TestCaseEntity.class);
            entity.setIsDelete(false);
            if(entity.getRequestData() == null){
                entity.setRequestData("{\"headers\":{},\"params\":{},\"data\":{},\"json\":{}}");
            }
            if(entity.getExtract() == null){
                entity.setExtract("[]");
            }
            if(entity.getAssertion() == null){
                entity.setAssertion("[]");
            }
            if(entity.getDbAssertion() == null){
                entity.setDbAssertion("[]");
            }
            if(entity.getOrderIndex() ==null){
                entity.setOrderIndex(10);
            }

            this.save(entity);

            TestCaseOutputDto outputDto = modelMapper.map(entity,TestCaseOutputDto.class);

            responseData = ResponseData.success(outputDto);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData<TestCaseOutputDto> update(TestCaseUpdateInputDto inputDto) {
        ResponseData<TestCaseOutputDto> responseData;

        try {
            List<String> checkMsgs = new ArrayList <>();
            //??????????????????????????????
            QueryWrapper<TestCaseEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", inputDto.getName());
            queryWrapper.eq("is_delete",false);
            queryWrapper.eq("interface_id",inputDto.getInterfaceId());
            queryWrapper.eq("test_suit_id",inputDto.getTestSuitId());
            queryWrapper.ne("id",inputDto.getId());
            TestCaseEntity testCaseEntity = this.getOne(queryWrapper,false);
            if (testCaseEntity!=null) {
                checkMsgs.add("??????????????????????????????");
            }
            //????????????????????????
            QueryWrapper<InterfaceEntity> interfaceEntityQueryWrapper = new QueryWrapper<>();
            interfaceEntityQueryWrapper.eq("id", inputDto.getInterfaceId());
            interfaceEntityQueryWrapper.eq("is_delete", false);
            Integer existCount = interfaceService.count(interfaceEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("?????????????????????");
            }
            //??????????????????????????????
            QueryWrapper<TestSuitEntity> testSuitEntityQueryWrapper = new QueryWrapper<>();
            testSuitEntityQueryWrapper.eq("id", inputDto.getTestSuitId());
            testSuitEntityQueryWrapper.eq("is_delete", false);
            existCount = testSuitService.count(testSuitEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("???????????????????????????");
            }
            if(checkMsgs.size()>0){
                responseData = new ResponseData <>();
                responseData.setCode(1);
                responseData.setMessage(checkMsgs.stream().collect(Collectors.joining(",")));

                return responseData;
            }

            //DTO???Entity
            TestCaseEntity entity = modelMapper.map(inputDto,TestCaseEntity.class);

            //????????????
            this.updateById(entity);


            //Entity???DTO
            TestCaseOutputDto outputDto = modelMapper.map(entity,TestCaseOutputDto.class);

            responseData = ResponseData.success(outputDto);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData<Boolean> delete(Integer id) {
        ResponseData<Boolean> responseData;

        try {
            List<String> checkMsgs = new ArrayList <>();
            //????????????????????????
            QueryWrapper<TestCaseEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id",id);
            queryWrapper.eq("is_delete",false);
            TestCaseEntity testCaseEntity = this.getOne(queryWrapper,false);
            if (testCaseEntity==null) {
                checkMsgs.add("?????????????????????");
            }
            if(checkMsgs.size()>0){
                responseData = new ResponseData <>();
                responseData.setCode(1);
                responseData.setMessage(checkMsgs.stream().collect(Collectors.joining(",")));

                return responseData;
            }

            testCaseEntity.setIsDelete(true);
            Boolean result = this.updateById(testCaseEntity);

            responseData = ResponseData.success(result);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

    @Override
    public ResponseData<TestCaseOutputDto> copy(TestCaseCreateInputDto inputDto) {
        ResponseData<TestCaseOutputDto> responseData;

        try {
            inputDto.setName(inputDto.getName()+"-??????");
            QueryWrapper<TestCaseEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.like("name", inputDto.getName());
            queryWrapper.eq("is_delete",false);
            queryWrapper.eq("interface_id",inputDto.getInterfaceId());
            queryWrapper.eq("test_suit_id",inputDto.getTestSuitId());
            List<TestCaseEntity> testCaseEntities = this.list(queryWrapper);
            //???????????????????????????????????????????????????[-??????]
            if (testCaseEntities!=null&& testCaseEntities.size()>0) {
                TestCaseEntity testCaseEntity =  testCaseEntities.stream().max((x,y)->{
                   if(x.getName().length()>y.getName().length()){
                       return 1;
                   }else{
                       return -1;
                   }
                }).get();
                inputDto.setName(testCaseEntity.getName()+"-??????");
            }

            List<String> checkMsgs = new ArrayList <>();
            //????????????????????????
            QueryWrapper<InterfaceEntity> interfaceEntityQueryWrapper = new QueryWrapper<>();
            interfaceEntityQueryWrapper.eq("id", inputDto.getInterfaceId());
            interfaceEntityQueryWrapper.eq("is_delete", false);
            Integer existCount = interfaceService.count(interfaceEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("?????????????????????");
            }
            //??????????????????????????????
            QueryWrapper<TestSuitEntity> testSuitEntityQueryWrapper = new QueryWrapper<>();
            testSuitEntityQueryWrapper.eq("id", inputDto.getTestSuitId());
            testSuitEntityQueryWrapper.eq("is_delete", false);
            existCount = testSuitService.count(testSuitEntityQueryWrapper);
            if(existCount<=0){
                checkMsgs.add("???????????????????????????");
            }
            if(checkMsgs.size()>0){
                responseData = new ResponseData <>();
                responseData.setCode(1);
                responseData.setMessage(checkMsgs.stream().collect(Collectors.joining(",")));

                return responseData;
            }

            TestCaseEntity entity = modelMapper.map(inputDto,TestCaseEntity.class);

            entity.setIsDelete(false);
            this.save(entity);
            TestCaseOutputDto outputDto = modelMapper.map(entity,TestCaseOutputDto.class);

            responseData = ResponseData.success(outputDto);
        }catch (Exception ex){
            log.error("???????????????",ex);
            responseData = ResponseData.failure("???????????????"+ex.toString());
        }

        return responseData;
    }

}
