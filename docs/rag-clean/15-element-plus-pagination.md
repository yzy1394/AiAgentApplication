# Pagination 分页 | Element Plus

- Source File: `docs/rag-web/15-element-plus-pagination.html`

- [ 100/page ]
- [ 200/page ]
- [ 300/page ]
- [ 400/page ]

- [ 100/page ]
- [ 200/page ]
- [ 300/page ]
- [ 400/page ]

# Pagination 分页

当数据量过多时，使用分页分解数据。

## 基础用法

设置`layout`，表示需要显示的内容，用逗号分隔，布局元素会依次显示。 分页元素包括：`prev`（跳转到上一页的按钮）、`next`（跳转到下一页的按钮）、`pager`（页码列表）、`jumper`（跳转输入框）、`total`（总条目数）、`sizes`（用于设置每页条数的选择器）以及 `->`（该符号之后的所有元素将被靠右对齐）。

- 1
- 2
- 3
- 4
- 5

- 1
- 2
- 3
- 4
- 5
- 6
- 100

```vue
<template>
  <div class="example-pagination-block">
    <div class="example-demonstration">When you have few pages</div>
    <el-pagination layout="prev, pager, next" :total="50" />
  </div>
  <div class="example-pagination-block">
    <div class="example-demonstration">When you have more than 7 pages</div>
    <el-pagination layout="prev, pager, next" :total="1000" />
  </div>
</template>

<style scoped>
.example-pagination-block + .example-pagination-block {
  margin-top: 10px;
}
.example-pagination-block .example-demonstration {
  margin-bottom: 16px;
}
</style>
```

## 设置最大页码按钮数

默认情况下，当总页数超过 7 页时，Pagination 会折叠多余的页码按钮。 通过 `pager-count` 属性可以设置最大页码按钮数。

- 1
- 2
- 3
- 4
- 5
- 6
- 7
- 8
- 9
- 10
- 50

```vue
<template>
  <el-pagination
    :page-size="20"
    :pager-count="11"
    layout="prev, pager, next"
    :total="1000"
  />
</template>
```

## 带有背景色的分页

设置`background`属性可以为分页按钮添加背景色。

- 1
- 2
- 3
- 4
- 5
- 6
- 100

```vue
<template>
  <el-pagination background layout="prev, pager, next" :total="1000" />
</template>
```

## 小型分页

在空间有限的情况下，可以使用简单的小型分页。

通过`size`更改大小 这是个 `small`的例子

- 1
- 2
- 3
- 4
- 5

- 1
- 2
- 3
- 4
- 5

```vue
<template>
  <el-pagination size="small" layout="prev, pager, next" :total="50" />
  <el-pagination
    size="small"
    background
    layout="prev, pager, next"
    :total="50"
    class="mt-4"
  />
</template>
```

## 当只有一页时隐藏分页

当只有一页时，通过设置 `hide-on-single-page` 属性来隐藏分页。

- 1

```vue
<template>
  <div>
    <el-switch v-model="value" />
    <hr class="my-4" />
    <el-pagination
      :hide-on-single-page="value"
      :total="5"
      layout="prev, pager, next"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const value = ref(false)
</script>
```

## 附加功能

根据场景需要，可以添加其他功能模块。

此示例是一个完整的用例。 使用了 `size-change` 和 `current-change` 事件来处理页码大小和当前页变动时候触发的事件。 `page-sizes`接受一个整数类型的数组，数组元素为展示的选择每页显示个数的选项，`[100, 200, 300, 400]` 表示四个选项，每页显示 100 个，200 个，300 个或者 400 个。

- 1
- 3
- 4
- 5
- 6
- 7
- 10

- 1
- 3
- 4
- 5
- 6
- 7
- 10

- 1
- 3
- 4
- 5
- 6
- 7
- 10

Go toinput [ prepend slot v-ifprefix slot v-if suffix slot v-if append slot v-if]

- 1
- 2
- 3
- 4

Go toinput [ prepend slot v-ifprefix slot v-if suffix slot v-if append slot v-if]

```vue
<template>
  <div class="flex items-center mb-4">
    <el-radio-group v-model="size" class="mr-4">
      <el-radio-button value="default">default</el-radio-button>
      <el-radio-button value="large">large</el-radio-button>

      <el-radio-button value="small">small</el-radio-button>
    </el-radio-group>
    <div>
      background:
      <el-switch v-model="background" class="ml-2" />
    </div>
    <div class="ml-4">
      disabled: <el-switch v-model="disabled" class="ml-2" />
    </div>
  </div>

  <hr class="my-4" />

  <div class="demo-pagination-block">
    <div class="demonstration">Total item count</div>
    <el-pagination
      v-model:current-page="currentPage1"
      :page-size="100"
      :size="size"
      :disabled="disabled"
      :background="background"
      layout="total, prev, pager, next"
      :total="1000"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
  <div class="demo-pagination-block">
    <div class="demonstration">Change page size</div>
    <el-pagination
      v-model:current-page="currentPage2"
      v-model:page-size="pageSize2"
      :page-sizes="[100, 200, 300, 400]"
      :size="size"
      :disabled="disabled"
      :background="background"
      layout="sizes, prev, pager, next"
      :total="1000"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
  <div class="demo-pagination-block">
    <div class="demonstration">Jump to</div>
    <el-pagination
      v-model:current-page="currentPage3"
      v-model:page-size="pageSize3"
      :size="size"
      :disabled="disabled"
      :background="background"
      layout="prev, pager, next, jumper"
      :total="1000"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
  <div class="demo-pagination-block">
    <div class="demonstration">All combined</div>
    <el-pagination
      v-model:current-page="currentPage4"
      v-model:page-size="pageSize4"
      :page-sizes="[100, 200, 300, 400]"
      :size="size"
      :disabled="disabled"
      :background="background"
      layout="total, sizes, prev, pager, next, jumper"
      :total="400"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

import type { ComponentSize } from 'element-plus'

const currentPage1 = ref(5)
const currentPage2 = ref(5)
const currentPage3 = ref(5)
const currentPage4 = ref(4)
const pageSize2 = ref(100)
const pageSize3 = ref(100)
const pageSize4 = ref(100)
const size = ref<ComponentSize>('default')
const background = ref(false)
const disabled = ref(false)

const handleSizeChange = (val: number) => {
  console.log(`${val} items per page`)
}
const handleCurrentChange = (val: number) => {
  console.log(`current page: ${val}`)
}
</script>

<style scoped>
.demo-pagination-block + .demo-pagination-block {
  margin-top: 10px;
}
.demo-pagination-block .demonstration {
  margin-bottom: 16px;
}
</style>
```

## API

### 属性

| 属性名 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| size 2.7.6 | 分页大小 | `enum` | 'default' |
| background | 是否为分页按钮添加背景色 | `boolean` | false |
| page-size / v-model:page-size | 每页显示条目个数 | `number` | — |
| default-page-size | 每页默认的条目个数，不设置时默认为10 | `number` | — |
| total | 总条目数 | `number` | — |
| page-count | 总页数， `total` 和 `page-count` 设置任意一个就可以达到显示页码的功能；如果要支持 `page-sizes` 的更改，则需要使用 `total` 属性 | `number` | — |
| pager-count | 设置最大页码按钮数。 页码按钮的数量，当总页数超过该值时会折叠 | `number` | 7 |
| current-page / v-model:current-page | 当前页数 | `number` | — |
| default-current-page | 当前页数的默认初始值，不设置时默认为 1 | `number` | — |
| layout | 组件布局，子组件名用逗号分隔 | `string` | prev, pager, next, jumper, ->, total |
| page-sizes | 每页显示个数选择器的选项设置 | `array` | [10, 20, 30, 40, 50, 100] |
| append-size-to 2.8.4 | 下拉框挂载到哪个 DOM 元素 | `string` | — |
| popper-class | 每页显示个数选择器的下拉框类名 | `string` | '' |
| popper-style 2.11.5 | 每页显示个数选择器的下拉框样式 | `string` / `object` | aaa |
| prev-text | 替代图标显示的上一页文字 | `string` | '' |
| prev-icon | 上一页的图标， 比 `prev-text` 优先级更高 | `string` / `Component` | ArrowLeft |
| next-text | 替代图标显示的下一页文字 | `string` | '' |
| next-icon | 下一页的图标， 比 `next-text` 优先级更低 | `string` / `Component` | ArrowRight |
| disabled | 是否禁用分页 | `boolean` | false |
| teleported 2.3.13 | 是否将下拉菜单teleport至 body | `boolean` | true |
| hide-on-single-page | 只有一页时是否隐藏 | `boolean` | false |
| small deprecated | 是否使用小型分页样式 | `boolean` | false |

WARNING

我们现在会检查一些不合理的用法，如果发现分页器未显示，可以核对是否违反以下情形：

- total 和 page-count 必须传一个，不然组件无法判断总页数；优先使用 page-count ;
- 如果传入了 current-page ，必须监听 current-page 变更的事件（ @update:current-page ），否则分页切换不起作用；
- 如果传入了 page-size ，且布局包含 page-size 选择器（即 layout 包含 sizes ），必须监听 page-size 变更的事件（ @update:page-size ），否则分页大小的变化将不起作用。

### 事件

| 名称 | 说明 | 类型 |
| --- | --- | --- |
| size-change | `page-size` 改变时触发 | `Function` |
| current-change | `current-page` 改变时触发 | `Function` |
| change 2.4.4 | `current-page` 或 `page-size` 更改时触发 | `Function` |
| prev-click | 用户点击上一页按钮改变当前页时触发 | `Function` |
| next-click | 用户点击下一页按钮改变当前页时触发 | `Function` |

WARNING

以上事件不推荐使用（但由于兼容的原因仍然支持，在以后的版本中将会被删除）；如果要监听 current-page 和 page-size 的改变，使用 `v-model` 双向绑定是个更好的选择。

### 插槽

| 名称 | 说明 |
| --- | --- |
| default | 自定义内容 设置文案，需要在 `layout` 中列出 `slot` |

## 源代码

组件 (https://github.com/element-plus/element-plus/tree/dev/packages/components/pagination) • 样式 (https://github.com/element-plus/element-plus/tree/dev/packages/theme-chalk/src/pagination.scss) • 文档 (https://github.com/element-plus/element-plus/blob/dev/docs/en-US/component/pagination.md)

## 贡献者
