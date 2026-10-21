package com.seckill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.UserContext;
import com.seckill.entity.SeckillOrder;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.service.SeckillOrderService;
import com.seckill.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 订单生命周期测试：支付幂等、超时关单、库存只回补一次
 */
class OrderLifecycleTest extends BaseIntegrationTest {

    @Autowired
    private SeckillOrderService orderService;

    @Autowired
    private SeckillOrderMapper orderMapper;

    private void insertOrder(Long userId, Long activityId, String orderNo) {
        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setGoodsId(1L);
        order.setAmount(new BigDecimal("99.00"));
        order.setStatus(0);
        orderMapper.insert(order);
    }

    private SeckillOrder findByOrderNo(String orderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getOrderNo, orderNo)
                .last("limit 1"));
    }

    @Test
    @DisplayName("支付：状态变已支付并记录支付时间，重复支付幂等")
    void payShouldBeIdempotent() {
        Long activityId = seedActivity(10);
        preheat(activityId, 10);
        String orderNo = "SKTEST-PAY-001";
        insertOrder(7L, activityId, orderNo);

        UserContext.set(new UserContext.CurrentUser(7L, "buyer", 0));
        try {
            orderService.pay(orderNo);

            SeckillOrder paid = findByOrderNo(orderNo);
            assertThat(paid.getStatus()).isEqualTo(1);
            assertThat(paid.getPayTime()).isNotNull();

            // 重复支付不应报错，也不应改变状态
            assertThatCode(() -> orderService.pay(orderNo)).doesNotThrowAnyException();
            assertThat(findByOrderNo(orderNo).getStatus()).isEqualTo(1);
        } finally {
            UserContext.clear();
        }
    }

    @Test
    @DisplayName("支付：他人订单按不存在处理，返回 404")
    void shouldNotPayOthersOrder() {
        Long activityId = seedActivity(10);
        preheat(activityId, 10);
        String orderNo = "SKTEST-PAY-002";
        insertOrder(8L, activityId, orderNo);

        UserContext.set(new UserContext.CurrentUser(9L, "stranger", 0));
        try {
            BusinessException error = catchThrowableOfType(
                    () -> orderService.pay(orderNo), BusinessException.class);

            assertThat(error).isNotNull();
            assertThat(error.getCode()).isEqualTo(404);
            assertThat(findByOrderNo(orderNo).getStatus()).isEqualTo(0);
        } finally {
            UserContext.clear();
        }
    }

    @Test
    @DisplayName("超时关单：库存只回补一次，重复消息不会重复加库存")
    void cancelShouldRestoreStockOnlyOnce() {
        Long activityId = seedActivity(10);
        // 模拟：10 件库存已经被扣成 5 件
        preheat(activityId, 5);
        String orderNo = "SKTEST-CANCEL-001";
        insertOrder(11L, activityId, orderNo);

        assertThat(orderService.cancelIfUnpaid(orderNo)).as("首次应取消成功").isTrue();
        assertThat(findByOrderNo(orderNo).getStatus()).isEqualTo(2);
        assertThat(redisStock(activityId)).isEqualTo("6");

        assertThat(orderService.cancelIfUnpaid(orderNo)).as("重复取消应返回 false").isFalse();
        assertThat(redisStock(activityId)).as("重复消息不能把库存加两次").isEqualTo("6");
    }

    @Test
    @DisplayName("超时关单：已支付订单不会被取消，库存不变")
    void shouldNotCancelPaidOrder() {
        Long activityId = seedActivity(10);
        preheat(activityId, 9);
        String orderNo = "SKTEST-CANCEL-002";
        insertOrder(12L, activityId, orderNo);

        UserContext.set(new UserContext.CurrentUser(12L, "buyer", 0));
        try {
            orderService.pay(orderNo);
        } finally {
            UserContext.clear();
        }

        assertThat(orderService.cancelIfUnpaid(orderNo)).isFalse();
        assertThat(findByOrderNo(orderNo).getStatus()).isEqualTo(1);
        assertThat(redisStock(activityId)).isEqualTo("9");
    }

    @Test
    @DisplayName("已取消订单不能再支付，返回 400")
    void shouldNotPayCancelledOrder() {
        Long activityId = seedActivity(10);
        preheat(activityId, 10);
        String orderNo = "SKTEST-PAY-003";
        insertOrder(13L, activityId, orderNo);
        orderService.cancelIfUnpaid(orderNo);

        UserContext.set(new UserContext.CurrentUser(13L, "buyer", 0));
        try {
            BusinessException error = catchThrowableOfType(
                    () -> orderService.pay(orderNo), BusinessException.class);

            assertThat(error).isNotNull();
            assertThat(error.getCode()).isEqualTo(400);
            assertThat(error.getMessage()).contains("已取消");
        } finally {
            UserContext.clear();
        }
    }
}
