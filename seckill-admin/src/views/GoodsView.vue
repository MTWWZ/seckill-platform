<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { createGoods, pageGoods, removeGoods, updateGoods, type Goods } from '@/api/goods'

const loading = ref(false)
const list = ref<Goods[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  category: ''
})

async function load() {
  loading.value = true
  try {
    const page = await pageGoods(query)
    list.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  load()
}

function handleReset() {
  query.keyword = ''
  query.category = ''
  handleSearch()
}

// ===== 新增 / 编辑 =====
const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const formRef = ref<FormInstance>()
const form = reactive<Goods>({
  name: '',
  title: '',
  description: '',
  imgUrl: '',
  originPrice: 0,
  stock: 0,
  category: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  originPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增商品'
  Object.assign(form, {
    id: undefined,
    name: '',
    title: '',
    description: '',
    imgUrl: '',
    originPrice: 0,
    stock: 0,
    category: '',
    status: 1
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function openEdit(row: Goods) {
  dialogTitle.value = '编辑商品'
  Object.assign(form, row)
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (form.id) {
    await updateGoods(form)
    ElMessage.success('修改成功（缓存已同步失效）')
  } else {
    await createGoods(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

async function handleDelete(row: Goods) {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
  await removeGoods(row.id!)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="按商品名称搜索"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.category"
          placeholder="按分类筛选"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
        <div class="spacer" />
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新增商品
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="商品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="title" label="卖点" min-width="130" show-overflow-tooltip />
        <el-table-column prop="originPrice" label="原价" width="100">
          <template #default="{ row }">￥{{ row.originPrice }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="category" label="分类" width="90" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：便携露营灯" />
        </el-form-item>
        <el-form-item label="卖点标题">
          <el-input v-model="form.title" placeholder="例如：三档亮度 · IPX4 防水" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="例如：户外" />
        </el-form-item>
        <el-form-item label="原价" prop="originPrice">
          <el-input-number v-model="form.originPrice" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="form.imgUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
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

.spacer {
  flex: 1;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
