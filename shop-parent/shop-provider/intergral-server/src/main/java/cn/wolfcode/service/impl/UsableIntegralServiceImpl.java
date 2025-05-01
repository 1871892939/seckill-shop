package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.AccountTransaction;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import cn.wolfcode.web.msg.IntergralCodeMsg;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.crypto.Data;
import java.util.Date;

/**
 * Created by lanxw
 */
@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;

    @Override
    public void decrIntegral(OperateIntergralVo vo) {
        int effectCount = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if (effectCount == 0) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
    }

    @Override
    public void incrIntegral(OperateIntergralVo vo) {
        usableIntegralMapper.incrIntergral(vo.getUserId(), vo.getValue());
    }

    @Override
    @Transactional
    public void decrIntegralTry(OperateIntergralVo vo, BusinessActionContext context) {
        System.out.println("execute try method");
        AccountTransaction log = new AccountTransaction();
        log.setTxId(context.getXid());
        log.setActionId(Long.valueOf(context.getBranchId()));
        Date now = new Date();
        log.setGmtCreated(now);
        log.setUserId(vo.getUserId());
        log.setAmount(vo.getValue());
        accountTransactionMapper.insert(log);

        int effectCount = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if (effectCount == 0) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
    }

    @Override
    public void decrIntegralCommit(BusinessActionContext context) {
        System.out.println("execute commit method");
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), Long.valueOf(context.getBranchId()));
        if (accountTransaction != null) {
            if (AccountTransaction.STATE_TRY == accountTransaction.getState()) {
                accountTransactionMapper.updateAccountTransactionState(context.getXid(), Long.valueOf(context.getBranchId()), AccountTransaction.STATE_COMMIT, AccountTransaction.STATE_TRY);
            } else if (AccountTransaction.STATE_COMMIT == accountTransaction.getState()){

            } else {

            }
        } else {

        }

    }

    @Override
    @Transactional
    public void decrIntegralRollback(BusinessActionContext context) {
        System.out.println("execute rollback method");
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), Long.valueOf(context.getBranchId()));
        if (accountTransaction != null) {
            if (AccountTransaction.STATE_TRY == accountTransaction.getState()) {
                accountTransactionMapper.updateAccountTransactionState(context.getXid(), Long.valueOf(context.getBranchId()), AccountTransaction.STATE_CANCEL, AccountTransaction.STATE_TRY);
//                String str = (String) context.getActionContext("vo");
                JSONObject jsonObj = (JSONObject) context.getActionContext("vo");
//                OperateIntergralVo vo = jsonObj.toJavaObject(OperateIntergralVo.class);
                System.out.println("storage context obj:"+jsonObj);
                usableIntegralMapper.incrIntergral(accountTransaction.getUserId(), accountTransaction.getAmount());
            } else if (AccountTransaction.STATE_CANCEL == accountTransaction.getState()) {

            } else {

            }
        } else {
//            String str = (String) context.getActionContext("vo");
//            System.out.println("storage context obj:"+str);
//            OperateIntergralVo vo = JSON.parseObject(str, OperateIntergralVo.class);

            JSONObject jsonObj = (JSONObject) context.getActionContext("vo");
            System.out.println("storage context obj:"+jsonObj);
            OperateIntergralVo vo = jsonObj.toJavaObject(OperateIntergralVo.class);
            AccountTransaction log = new AccountTransaction();
            log.setTxId(context.getXid());
            log.setActionId(Long.valueOf(context.getBranchId()));
            Date now = new Date();
            log.setGmtCreated(now);
            log.setGmtModified(now);
            log.setUserId(vo.getUserId());
            log.setAmount(vo.getValue());
            log.setState(AccountTransaction.STATE_CANCEL);
            accountTransactionMapper.insert(log);
        }
    }
}
