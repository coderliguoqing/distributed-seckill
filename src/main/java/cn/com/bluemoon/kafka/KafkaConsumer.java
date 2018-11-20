package cn.com.bluemoon.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.com.bluemoon.redis.lock.DistributedExclusiveRedisLock;
import cn.com.bluemoon.redis.repository.RedisRepository;
import cn.com.bluemoon.utils.SerialNo;

/**
 * 消费者 spring-kafka 2.0 + 依赖JDK8
 * @author Guoqing
 */
@Component
public class KafkaConsumer {
	
	private Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
	@Autowired
	private RedisRepository redisRepository;
	@Autowired
	private StringRedisTemplate redisTemplate;
	
    /**
     * 监听seckill主题,有消息就读取
     * 主要消费秒杀进入到下订单操作的队列数据，此处的数据已经过滤了绝大部分请求，只有真正得到下单机会的用户才会进入到这一环节
     * @param message
     */
    @KafkaListener(topics = {"demo_seckill"})
    public void receiveMessage(String message){
    	try {
			//收到通道的消息之后执行秒杀操作
			logger.info(message);
			JSONObject json = JSONObject.parseObject(message);
			long stallActivityId = json.getLong("stallActivityId");
			int purchaseNum = json.getInteger("purchaseNum");
			String openId = json.getString("openId");
//			long addressId = json.getLong("addressId");
//			String formId = json.getString("formId");
//			String shareCode = json.getString("shareCode");
//			String shareSource = json.getString("shareSource");
//			String userCode = json.getString("userCode");
			//生成订单，模拟生成订单编码
			String orderCode = "J"+SerialNo.getUNID();
			//删除redis中的key，让轮询接口发现，该订单已经处理完成
			redisRepository.del("BM_MARKET_LOCK_POLLING_" + stallActivityId + "_" + openId);
			//并将orderId_orderCode放入缓存，有效时间10分钟（因为支付有效时间为10分钟）
			redisRepository.setExpire("BM_MARKET_SECKILL_ORDERID_" + stallActivityId + "_" + openId, orderCode, 600);
			
			DistributedExclusiveRedisLock lock = new DistributedExclusiveRedisLock(redisTemplate); //构造锁的时候需要带入RedisTemplate实例
			lock.setLockKey("marketOrder"+stallActivityId); //控制锁的颗粒度(摊位活动ID)
			lock.setExpires(1L); //每次操作预计的超时时间,单位秒
			try{
				lock.lock();
				//扣减真实库存
				redisRepository.decrBy("BM_MARKET_SECKILL_REAL_STOCKNUM_" + stallActivityId, purchaseNum);
			}finally{
				lock.unlock();
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
    }
    
}