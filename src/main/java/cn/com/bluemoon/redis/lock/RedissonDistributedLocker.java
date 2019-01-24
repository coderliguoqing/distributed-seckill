package cn.com.bluemoon.redis.lock;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 基于Redisson的分布式锁实现
 * @author Guoqing.Lee
 * @date 2019年1月23日 下午4:04:57
 *
 */
@Component
public class RedissonDistributedLocker {
	
	@Autowired
	private RedissonClient redissonClient;
	
	/**
	 * 加锁
	 * @param lockKey
	 * @return
	 */
	public RLock lock(String lockKey) {
		RLock lock = redissonClient.getLock(lockKey);
		lock.lock();
		return lock;
	}
	
	/**
	 * 加锁，过期自动释放
	 * @param lockKey
	 * @param leaseTime	自动释放锁时间
	 * @return
	 */
	public RLock lock(String lockKey, long leaseTime) {
		RLock lock = redissonClient.getLock(lockKey);
		lock.lock(leaseTime, TimeUnit.SECONDS);
		return lock;
	}
	
	/**
	 * 加锁，过期自动释放，时间单位传入
	 * @param lockKey
	 * @param unit		时间单位
	 * @param leaseTime	上锁后自动释放时间
	 * @return
	 */
	public RLock lock(String lockKey, TimeUnit unit, long leaseTime) {
		RLock lock = redissonClient.getLock(lockKey);
		lock.lock(leaseTime, unit);
		return lock;
	}
	
	/**
	 * 尝试获取所
	 * @param lockKey
	 * @param unit		时间单位
	 * @param waitTime	最多等待时间
	 * @param leaseTime	上锁后自动释放时间
	 * @return
	 */
	public boolean tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) {
		RLock lock = redissonClient.getLock(lockKey);
		try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            return false;
        }
	}
	
	/**
	 * 尝试获取所
	 * @param lockKey
	 * @param waitTime	最多等待时间
	 * @param leaseTime	上锁后自动释放锁时间
	 * @return
	 */
	public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
		RLock lock = redissonClient.getLock(lockKey);
		try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
	}
	
	/**
	 * 释放锁
	 * @param lockKey
	 */
	public void unlock(String lockKey) {
		RLock lock = redissonClient.getLock(lockKey);
		lock.unlock();
	}
	
	/**
	 * 释放锁
	 * @param lock
	 */
	public void unlock(RLock lock) {
		lock.unlock();
	}
	
}
