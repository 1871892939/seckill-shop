package cn.wolfcode.service;

import cn.wolfcode.domain.OperateIntergralVo;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Created by lanxw
 */
@LocalTCC
public interface IUsableIntegralService {
    void decrIntegral(OperateIntergralVo vo);

    void incrIntegral(OperateIntergralVo vo);

    @TwoPhaseBusinessAction(name = "decrIntegralTry", commitMethod = "decrIntegralCommit", rollbackMethod = "decrIntegralRollback")
    void decrIntegralTry(@BusinessActionContextParameter(paramName = "vo") OperateIntergralVo vo, BusinessActionContext context);
    void decrIntegralCommit(BusinessActionContext context);
    void decrIntegralRollback(BusinessActionContext context);
}
