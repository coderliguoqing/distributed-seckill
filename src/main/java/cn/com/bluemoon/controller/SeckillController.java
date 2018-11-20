/**  
* <p>Title: SeckillApiController.java</p>  
* <p>Description: </p>  
* <p>Copyright: Copyright (c) 2018</p>  
* <p>Company: www.bluemoon.com</p>  
* @author Guoqing  
* @date 2018年8月10日  
*/  
package cn.com.bluemoon.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;

import cn.com.bluemoon.common.response.BaseResponse;
import cn.com.bluemoon.common.response.SeckillInfoResponse;
import cn.com.bluemoon.common.response.StockNumResponse;
import cn.com.bluemoon.redis.repository.RedisRepository;
import cn.com.bluemoon.service.ISeckillService;
import cn.com.bluemoon.utils.AssertUtil;
import cn.com.bluemoon.utils.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**  
* <p>Title: SeckillApiController</p>  
* <p>Description: 秒杀相关接口</p>  
* @author Guoqing  
* @date 2018年8月10日  
*/
@Api(tags="分布式秒杀")
@RestController
@CrossOrigin
@RequestMapping(value = "/api/seckill")
public class SeckillController {
	
	@Autowired
	private RedisRepository redisRepository;
	@Autowired
	private ISeckillService seckillService;

	Logger logger = LoggerFactory.getLogger(SeckillController.class);
	
	/**
	 * 设置活动库存
	 * @param jsonObject
	 * @return
	 */
	@ApiOperation(value="设置活动库存",nickname="Guoqing")
	@RequestMapping(value="/setStockNum", method=RequestMethod.POST)
	public BaseResponse setStockNum(@RequestBody JSONObject jsonObject) {
		
		int stockNum = jsonObject.containsKey("stockNum")?jsonObject.getInteger("stockNum"):0;
		int stallActivityId = jsonObject.containsKey("stallActivityId") ? jsonObject.getInteger("stallActivityId") : -1;
		AssertUtil.isTrue(stallActivityId != -1, "非法参数");
		redisRepository.incrBy("BM_MARKET_SECKILL_STOCKNUM_" + stallActivityId, stockNum);
		redisRepository.incrBy("BM_MARKET_SECKILL_REAL_STOCKNUM_" + stallActivityId, stockNum);
		
		return new BaseResponse();
	}
	
	/**
	 * 查看活动库存情况
	 * @param jsonObject
	 * @return
	 */
	@ApiOperation(value="查看活动库存",nickname="Guoqing")
	@RequestMapping(value="/getStockNum", method=RequestMethod.POST)
	public StockNumResponse getStockNum(@RequestBody JSONObject jsonObject) {
		StockNumResponse response = new StockNumResponse();
		int stallActivityId = jsonObject.containsKey("stallActivityId") ? jsonObject.getInteger("stallActivityId") : -1;
		AssertUtil.isTrue(stallActivityId != -1, "非法参数");
		String stockNum = redisRepository.get("BM_MARKET_SECKILL_STOCKNUM_" + stallActivityId);
		String realStockNum = redisRepository.get("BM_MARKET_SECKILL_REAL_STOCKNUM_" + stallActivityId);
		response.setStockNum(Long.parseLong(stockNum));
		response.setRealStockNum(Long.parseLong(realStockNum));
		return response;
	}

	/**
	 * 06.04-去秒杀，创建秒杀订单
	 * <p>Title: testSeckill</p>  
	 * <p>Description: 秒杀下单</p>  
	 * @param jsonObject
	 * @return
	 */
	@ApiOperation(value="去秒杀",nickname="Guoqing")
	@RequestMapping(value="/goSeckill", method=RequestMethod.POST)
	public SeckillInfoResponse goSeckill(@RequestBody JSONObject jsonObject) {
		int stallActivityId = jsonObject.containsKey("stallActivityId") ? jsonObject.getInteger("stallActivityId") : -1;		//活动Id
		AssertUtil.isTrue(stallActivityId != -1, "非法參數");
		int purchaseNum = jsonObject.containsKey("purchaseNum") ? jsonObject.getInteger("purchaseNum") : 1;		//购买数量
		AssertUtil.isTrue(purchaseNum != -1, "非法參數");
		String openId = jsonObject.containsKey("openId") ? jsonObject.getString("openId") : null;
		AssertUtil.isTrue(!StringUtil.isEmpty(openId), 1101, "非法參數");
		String formId = jsonObject.containsKey("formId") ? jsonObject.getString("formId") : null;
		AssertUtil.isTrue(!StringUtil.isEmpty(formId), 1101, "非法參數");
		long addressId = jsonObject.containsKey("addressId") ? jsonObject.getLong("addressId") : -1;
		AssertUtil.isTrue(addressId != -1, "非法參數");
		//通过分享入口进来的参数
		String shareCode =  jsonObject.getString("shareCode");
        String shareSource =  jsonObject.getString("shareSource");
        String userCode =  jsonObject.getString("userId");
		
		return seckillService.startSeckill(stallActivityId, purchaseNum, openId, formId, addressId, shareCode, shareSource, userCode);
	}
	
	/**
	 * 06.05-轮询请求当前用户是否秒杀下单成功
	 * <p>Title: seckillPolling</p>  
	 * <p>Description: </p>  
	 * @param jsonObject
	 * @return
	 */
	@ApiOperation(value="轮询接口",nickname="Guoqing")
	@RequestMapping(value="/seckillPolling", method=RequestMethod.POST)
	public SeckillInfoResponse seckillPolling(@RequestBody JSONObject jsonObject) {
		int stallActivityId = jsonObject.containsKey("stallActivityId") ? jsonObject.getInteger("stallActivityId") : -1;		//活动Id
		AssertUtil.isTrue(stallActivityId != -1, "非法參數");
		String openId = jsonObject.containsKey("openId") ? jsonObject.getString("openId") : null;
		AssertUtil.isTrue(!StringUtil.isEmpty(openId), 1101, "非法參數");
		
		SeckillInfoResponse response = new SeckillInfoResponse();
		if( redisRepository.exists("BM_MARKET_LOCK_POLLING_" + stallActivityId + "_" + openId) ) {
			//如果缓存中存在锁定秒杀和用户ID的key，则证明该订单尚未处理完成，需要继续等待
			response.setIsSuccess(true);
			response.setResponseCode(6103);
			response.setResponseMsg("排队中，请稍后");
			response.setRefreshTime(1000);
		} else {
			//如果缓存中该key已经不存在，则表明该订单已经下单成功，可以进入支付操作，并取出orderId返回
			String redisOrderInfo = redisRepository.get("BM_MARKET_SECKILL_ORDERID_" + stallActivityId + "_" + openId);
			if( redisOrderInfo == null ) {
				response.setIsSuccess(false);
				response.setResponseCode(6106);
				response.setResponseMsg("秒杀失败，下单出现异常，请重试！");
				response.setOrderCode(null);
				response.setRefreshTime(0);
			}else {
				response.setIsSuccess(true);
				response.setResponseCode(6104);
				response.setResponseMsg("秒杀成功");
				response.setOrderCode(redisOrderInfo);
				response.setRefreshTime(0);
			}
		}
		return response;
	}
	
}
