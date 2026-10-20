<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { createActivity, pageActivity, preheatActivity, type SeckillActivity } from '@/api/activity'

const loading = ref(false)
const list = ref<SeckillActivity[]>([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const page = await pageActivity(query)
    list.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  goodsId: undefined as number | undefined,
  seckillPrice: 0,
  seckillStock: 10,
  startTime: '',
  endTime: ''
})

const rules = {
  goodsId: [{ required: true, message: '请输入商品 ID', trigger: 'blur' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  seckillStock: [{ required: true, message: '请输入活动库存', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, {
    goodsId: undefined,
    seckillPrice: 0,
    seckillStock: 10,
    startTime: '',
    endTime: ''
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  await createActivity(form as { goodsId: number; seckillPrice: number; seckillStock: number; startTime: string; endTime: string })
  ElMessage.success('创建成功，库存已自动预热到 Redis')
  dialogVisible.value = false
  load()
}

async function handlePreheat(row: SeckillActivity) {
  await preheatActivity(row.id)
  ElMessage.success('已重新预热库存到 Redis')
  load()
}

function statusTagType(text: string) {
  if (text === '进行中') return 'success'
  if (text === '未开始') return 'warning'
  return 'info'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="hint">
          活动创建后会自动把库存预热到 Redis，抢购时直接扣 Redis，不碰数据库
        </span>
        <div class="spacer" />
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新建活动
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="goodsName" label="关联商品" min-width="150" show-overflow-tooltip />
        <el-table-column label="原价 / 秒杀价" width="150">
          <template #default="{ row }">
            <span class="origin">￥{{ row.originPrice }}</span>
            <span class="seckill">￥{{ row.seckillPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="seckillStock" label="活动库存" width="90" />
        <el-table-column label="Redis 实时库存" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.redisStock === null" type="info">未预热</el-tag>
            <el-tag v-else type="success">{{ row.redisStock }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.statusText)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="起止时间" min-width="260">
          <template #default="{ row }">
            <div class="time">{{ row.startTime?.replace('T', ' ') }}</div>
            <div class="time">~ {{ row.endTime?.replace('T', ' ') }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePreheat(row)">预热</el-button>
          </template>
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

    <el-dialog v-model="dialogVisible" title="新建秒杀活动" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="商品 ID" prop="goodsId">
          <el-input-number v-model="form.goodsId" :min="1" placeholder="对应商品管理里的 ID" />
        </el-form-item>
        <el-form-item label="秒杀价" prop="seckillPrice">
          <el-input-number v-model="form.seckillPrice" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="活动库存" prop="seckillStock">
          <el-input-number v-model="form.seckillStock" :min="1" :step="5" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">创建</el-button>
      </template>
    </el-dialog>
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

.origin {
  color: #909399;
  text-decoration: line-through;
  margin-right: 6px;
}

.seckill {
  color: #f56c6c;
  font-weight: 600;
}

.time {
  font-size: 12px;
  color: #606266;
}
</style>
