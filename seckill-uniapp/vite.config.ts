import { createRequire } from 'node:module'
import { defineConfig } from 'vite'

// @dcloudio/vite-plugin-uni 是 CJS 产物（exports.default = uniPlugin）。
// 在本项目的 ESM 配置里直接 `import uni from ...` 拿到的是整个 module.exports 对象，
// 会报 "uni is not a function"，所以用 createRequire 取真正的导出。
const require = createRequire(import.meta.url)
const uni = require('@dcloudio/vite-plugin-uni').default

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5273,
    // H5 开发环境把 /api 转发到后端，绕开浏览器跨域限制
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
