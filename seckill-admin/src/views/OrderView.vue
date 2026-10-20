<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { pageOrder, type AdminOrder } from '@/api/order'

const loading = ref(false)
const list = ref<AdminOrder[]>([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const page = await pageOrder(query)
    list.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function statusTagType(status: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  if (status === 3) return 'success'
  return 'warning'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="hint">
          订单由 RabbitMQ 消费者异步落库，所以刚抢完时这里可能少几条，稍等片刻就会补齐
        </span>
        <div class="spacer" />
        <el-button @click="load">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
        <el-table-column label="用户" width="140">
          <template #default="{ row }">
            <span>{{ row.nickname || row.username || row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="goodsName" label="商品" min-width="140" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">￥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ row.createTime?.replace('T', ' ') }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @size-change="load"
        @current-change="load"
      />
    </el-card>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.hint {
  color: #909399;
  font-size: 13px;
}

.spacer {
  flex: 1;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
