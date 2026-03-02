# Dialog 对话框 | Element Plus

- Source File: `docs/rag-web/16-element-plus-dialog.html`

# Dialog 对话框

在保留当前页面状态的情况下，告知用户并承载相关操作。

## 基础用法

Dialog 弹出一个对话框，适合需要定制性更大的场景。

需要设置 `model-value / v-model` 属性，它接收 `Boolean`，当为 `true` 时显示 Dialog。 Dialog 分为两个部分：`body` 和 `footer`，`footer` 需要具名为 `footer` 的 `slot`。 `title` 属性用于定义标题，它是可选的，默认值为空。 最后，本例还展示了 `before-close` 的用法。

```vue
<template>
  <el-button plain @click="dialogVisible = true">
    Click to open the Dialog
  </el-button>

  <el-dialog
    v-model="dialogVisible"
    title="Tips"
    width="500"
    :before-close="handleClose"
  >
    <span>This is a message</span>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="dialogVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'

const dialogVisible = ref(false)

const handleClose = (done: () => void) => {
  ElMessageBox.confirm('Are you sure to close this dialog?')
    .then(() => {
      done()
    })
    .catch(() => {
      // catch error
    })
}
</script>
```

TIP

`before-close` 只会在用户点击关闭按钮或者对话框的遮罩区域时被调用。 如果你在 `footer` 具名 slot 里添加了用于关闭 Dialog 的按钮，那么可以在按钮的点击回调函数里加入 `before-close` 的相关逻辑。

## 自定义内容

对话框的内容可以是任何东西，甚至是一个表格或表单。 此示例显示如何在 Dialog 中使用 Element Plus 的表格和表单。

```vue
<template>
  <div class="flex flex-wrap gap-1">
    <el-button class="!ml-0" plain @click="dialogTableVisible = true">
      Open a Table nested Dialog
    </el-button>

    <el-button class="!ml-0" plain @click="dialogFormVisible = true">
      Open a Form nested Dialog
    </el-button>
  </div>

  <el-dialog v-model="dialogTableVisible" title="Shipping address" width="800">
    <el-table :data="gridData">
      <el-table-column property="date" label="Date" width="150" />
      <el-table-column property="name" label="Name" width="200" />
      <el-table-column property="address" label="Address" />
    </el-table>
  </el-dialog>

  <el-dialog v-model="dialogFormVisible" title="Shipping address" width="500">
    <el-form :model="form">
      <el-form-item label="Promotion name" :label-width="formLabelWidth">
        <el-input v-model="form.name" autocomplete="off" />
      </el-form-item>
      <el-form-item label="Zones" :label-width="formLabelWidth">
        <el-select v-model="form.region" placeholder="Please select a zone">
          <el-option label="Zone No.1" value="shanghai" />
          <el-option label="Zone No.2" value="beijing" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogFormVisible = false">Cancel</el-button>
        <el-button type="primary" @click="dialogFormVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'

const dialogTableVisible = ref(false)
const dialogFormVisible = ref(false)
const formLabelWidth = '140px'

const form = reactive({
  name: '',
  region: '',
  date1: '',
  date2: '',
  delivery: false,
  type: [],
  resource: '',
  desc: '',
})

const gridData = [
  {
    date: '2016-05-02',
    name: 'John Smith',
    address: 'No.1518,  Jinshajiang Road, Putuo District',
  },
  {
    date: '2016-05-04',
    name: 'John Smith',
    address: 'No.1518,  Jinshajiang Road, Putuo District',
  },
  {
    date: '2016-05-01',
    name: 'John Smith',
    address: 'No.1518,  Jinshajiang Road, Putuo District',
  },
  {
    date: '2016-05-03',
    name: 'John Smith',
    address: 'No.1518,  Jinshajiang Road, Putuo District',
  },
]
</script>
```

## 自定义头部

`header` 可用于自定义显示标题的区域。 为了保持可用性，除了使用此插槽外，使用 `title` 属性，或使用 `titleId` 插槽属性来指定哪些元素应该读取为对话框标题。

```vue
<template>
  <el-button plain @click="visible = true">
    Open Dialog with customized header
  </el-button>

  <el-dialog v-model="visible" :show-close="false" width="500">
    <template #header="{ close, titleId, titleClass }">
      <div class="my-header">
        <h4 :id="titleId" :class="titleClass">This is a custom header!</h4>
        <el-button type="danger" @click="close">
          <el-icon class="el-icon--left"><CircleCloseFilled /></el-icon>
          Close
        </el-button>
      </div>
    </template>
    This is dialog content.
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { CircleCloseFilled } from '@element-plus/icons-vue'

const visible = ref(false)
</script>

<style scoped>
.my-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 16px;
}
</style>
```

## 嵌套的对话框

如果需要在一个 Dialog 内部嵌套另一个 Dialog，需要使用 `append-to-body` 属性。

通常我们不建议使用嵌套对话框。 如果你需要在页面上呈现多个对话框，你可以简单地打平它们，以便它们彼此之间是平级关系。 如果必须要在一个对话框内展示另一个对话框，可以将内部嵌套的对话框属性 `append-to-body` 设置为 true，嵌套的对话框将附加到 body 而不是其父节点，这样两个对话框都可以被正确地渲染。

```vue
<template>
  <el-button plain @click="outerVisible = true">
    Open the outer Dialog
  </el-button>

  <el-dialog v-model="outerVisible" title="Outer Dialog" width="800">
    <span>This is the outer Dialog</span>
    <el-dialog
      v-model="innerVisible"
      width="500"
      title="Inner Dialog"
      append-to-body
    >
      <span>This is the inner Dialog</span>
    </el-dialog>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="outerVisible = false">Cancel</el-button>
        <el-button type="primary" @click="innerVisible = true">
          Open the inner Dialog
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const outerVisible = ref(false)
const innerVisible = ref(false)
</script>
```

## 内容居中

对话框的内容可以居中。

将`center`设置为`true`即可使标题和底部居中。 `center`仅影响标题和底部区域。 Dialog 的内容是任意的，在一些情况下，内容并不适合居中布局。 如果需要内容也水平居中，请自行为其添加 CSS 样式。

```vue
<template>
  <el-button plain @click="centerDialogVisible = true">
    Click to open the Dialog
  </el-button>

  <el-dialog v-model="centerDialogVisible" title="Warning" width="500" center>
    <span>
      It should be noted that the content will not be aligned in center by
      default
    </span>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="centerDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="centerDialogVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const centerDialogVisible = ref(false)
</script>
```

TIP

Dialog 的内容是懒渲染的——在被第一次打开之前，传入的默认 slot 不会被立即渲染到 DOM 上。 因此，如果需要执行 DOM 操作，或通过 `ref` 获取相应组件，请在 `open` 事件回调中进行。

## 居中对话框

从屏幕中心打开对话框。

设置 `align-center` 为 `true` 使对话框水平垂直居中。 由于对话框垂直居中在弹性盒子中，所以`top`属性将不起作用。

```vue
<template>
  <el-button plain @click="centerDialogVisible = true">
    Click to open the Dialog
  </el-button>

  <el-dialog
    v-model="centerDialogVisible"
    title="Warning"
    width="500"
    align-center
  >
    <span>Open the dialog from the center from the screen</span>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="centerDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="centerDialogVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const centerDialogVisible = ref(false)
</script>
```

## 关闭时销毁

启用此功能时，默认栏位下的内容将使用 `v-if` 指令销毁。 当出现性能问题时，可以启用此功能。

需要注意的是，当这个属性被启用时，在 `transition.beforeEnter` 事件卸载前，除了 `overlay`、`header (可选)`与`footer(可选)` ，Dialog 内不会有其它任何其它的 DOM 节点存在。

```vue
<template>
  <el-button plain @click="centerDialogVisible = true">
    Click to open Dialog
  </el-button>

  <el-dialog
    v-model="centerDialogVisible"
    title="Notice"
    width="500"
    destroy-on-close
    center
  >
    <span>
      Notice: before the dialog is opened for the first time, this node and the
      one below will not be rendered.
    </span>
    <div>
      <strong>Extra content (Not rendered)</strong>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="centerDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="centerDialogVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const centerDialogVisible = ref(false)
</script>
```

## 可拖拽对话框

试着拖动一下`header`部分吧

设置`draggable`属性为`true`以做到拖拽 设置 `overflow` 2.5.4 为 `true` 可以让拖拽范围超出可视区。

```vue
<template>
  <div class="flex flex-wrap gap-1">
    <el-button class="!ml-0" plain @click="dialogVisible = true">
      Open a draggable Dialog
    </el-button>
    <el-button class="!ml-0" plain @click="dialogOverflowVisible = true">
      Open a overflow draggable Dialog
    </el-button>
    <el-button class="!ml-0" plain @click="customDraggingVisible = true">
      Open a custom dragging style Dialog
    </el-button>
  </div>

  <el-dialog v-model="dialogVisible" title="Tips" width="500" draggable>
    <span>It's a draggable Dialog</span>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="dialogVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog
    v-model="dialogOverflowVisible"
    title="Tips"
    width="500"
    draggable
    overflow
  >
    <span>It's a overflow draggable Dialog</span>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogOverflowVisible = false">Cancel</el-button>
        <el-button type="primary" @click="dialogOverflowVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog
    v-model="customDraggingVisible"
    class="custom-dragging-style"
    title="Custom Dragging Style"
    width="500"
    draggable
  >
    <span
      >This dialog has custom dragging styles. Try dragging it to see the
      effects!</span
    >
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="customDraggingVisible = false">Cancel</el-button>
        <el-button type="primary" @click="customDraggingVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const dialogVisible = ref(false)
const dialogOverflowVisible = ref(false)
const customDraggingVisible = ref(false)
</script>

<style scoped>
:global(.custom-dragging-style.is-dragging) {
  border: 2px dashed var(--el-color-primary);
  opacity: 0.65;
}
</style>
```

TIP

当 `modal` 的值为 false 时，请一定要确保 `append-to-body` 属性为 true，由于 `Dialog` 使用 `position: relative` 定位，当外层的遮罩层被移除时，`Dialog` 则会根据当前 DOM 上的祖先节点来定位，因此可能造成定位问题。

## 全屏

设置 `fullscreen` 属性来打开全屏对话框。

```vue
<template>
  <el-button plain @click="dialogVisible = true">
    Open the fullscreen Dialog
  </el-button>

  <el-dialog v-model="dialogVisible" fullscreen>
    <span>It's a fullscreen Dialog</span>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="dialogVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const dialogVisible = ref(false)
</script>
```

TIP

如果 `fullscreen` 为 true，则 `width`、`top` 和 `draggable` 属性无效。

## 模态框

将 `modal` 设置为 `false` 会隐藏对话框的模态（覆盖层）。

从版本 2.10.5 起，新增了 `modal-penetrable`属性，该属性可设置为“可穿透”（即允许穿透）。

```vue
<template>
  <el-button plain @click="dialogVisible = true">
    Open the modal Dialog
  </el-button>

  <el-dialog v-model="dialogVisible" :modal="false" modal-penetrable>
    <span>It's a modal Dialog</span>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="dialogVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const dialogVisible = ref(false)
</script>
```

## 自定义动画 2.10.5

通过 `transition` 属性自定义对话框动画，该属性可以接受以下任意一种值：

- 动画名称（字符串）
- Vue 过渡配置（对象）

示例包括缩放（scale）、滑动（slide）、淡入淡出（fade）、弹跳（bounce）动画，以及带有自定义事件处理器的基于对象的配置。

```vue
<template>
  <div>
    <el-button plain @click="openDialog('fade')"> Default </el-button>
    <el-button plain @click="openDialog('scale')"> Scale </el-button>
    <el-button plain @click="openDialog('slide')"> Slide </el-button>
    <el-button plain @click="openDialog('bounce')"> Bounce </el-button>
    <el-button plain @click="openDialogWithObject"> Object Config </el-button>
  </div>

  <el-dialog
    v-model="dialogVisible"
    class="custom-transition-dialog"
    :title="`${currentAnimation} Animation Dialog`"
    width="30%"
    :transition="transitionConfig"
  >
    <div>
      <p>
        Current animation: <strong>{{ currentAnimation }}</strong>
      </p>
      <p>
        This dialog demonstrates the {{ currentAnimation }} animation effect.
      </p>
      <p v-if="isObjectConfig">
        <strong>Using object configuration:</strong><br />
        <code>{{ JSON.stringify(transitionConfig, null, 2) }}</code>
      </p>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">Cancel</el-button>
      <el-button type="primary" @click="dialogVisible = false">
        Confirm
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'

import type { DialogTransition } from 'element-plus'

const dialogVisible = ref(false)
const currentAnimation = ref('fade')
const isObjectConfig = ref(false)

const transitionConfig = computed<DialogTransition>(() => {
  if (isObjectConfig.value) {
    return {
      name: 'dialog-custom-object',
      appear: true,
      mode: 'out-in',
      duration: 500,
    }
  }
  return `dialog-${currentAnimation.value}`
})

const openDialog = (type: string) => {
  currentAnimation.value = type
  isObjectConfig.value = false
  dialogVisible.value = true
}

const openDialogWithObject = () => {
  currentAnimation.value = 'object-config'
  isObjectConfig.value = true
  dialogVisible.value = true
}
</script>

<style scoped>
code {
  background: var(--el-bg-color-page);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  display: block;
  margin-top: 8px;
}
</style>

<style>
/* Scale Animation */
.dialog-scale-enter-active,
.dialog-scale-leave-active,
.dialog-scale-enter-active .el-dialog,
.dialog-scale-leave-active .el-dialog {
  transition: all 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.dialog-scale-enter-from,
.dialog-scale-leave-to {
  opacity: 0;
}

.dialog-scale-enter-from .el-dialog,
.dialog-scale-leave-to .el-dialog {
  transform: scale(0.5);
  opacity: 0;
}

/* Slide Animation */
.dialog-slide-enter-active,
.dialog-slide-leave-active,
.dialog-slide-enter-active .el-dialog,
.dialog-slide-leave-active .el-dialog {
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.dialog-slide-enter-from,
.dialog-slide-leave-to {
  opacity: 0;
}

.dialog-slide-enter-from .el-dialog,
.dialog-slide-leave-to .el-dialog {
  transform: translateY(-100px);
  opacity: 0;
}

/* Bounce Animation */
.dialog-bounce-enter-active,
.dialog-bounce-leave-active,
.dialog-bounce-enter-active .el-dialog,
.dialog-bounce-leave-active .el-dialog {
  transition: all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.dialog-bounce-enter-from,
.dialog-bounce-leave-to {
  opacity: 0;
}

.dialog-bounce-enter-from .el-dialog,
.dialog-bounce-leave-to .el-dialog {
  transform: scale(0.3) translateY(-50px);
  opacity: 0;
}

/* Object Configuration Animation */
.dialog-custom-object-enter-active,
.dialog-custom-object-leave-active,
.dialog-custom-object-enter-active .el-dialog,
.dialog-custom-object-leave-active .el-dialog {
  transition: all 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.dialog-custom-object-enter-from,
.dialog-custom-object-leave-to {
  opacity: 0;
}

.dialog-custom-object-enter-from .el-dialog,
.dialog-custom-object-leave-to .el-dialog {
  transform: rotate(180deg) scale(0.5);
  opacity: 0;
}
</style>
```

TIP

动画类会根据过渡名称动态生成。 为了更细致地控制动画行为，你可以明确地定义这些类。 详情请参见 自定义过渡类（custom-transition-classes） (https://vuejs.org/guide/built-ins/transition.html#custom-transition-classes)。

## Events

打开开发者控制台(ctrl + shift + J)，查看事件的顺序。

```vue
<template>
  <el-button plain @click="dialogVisible = true">
    Open the event Dialog
  </el-button>

  <el-dialog
    v-model="dialogVisible"
    modal-class="overide-animation"
    :before-close="
      (doneFn) => {
        ;(console.log('before-close'), doneFn())
      }
    "
    @open="console.log('open')"
    @open-auto-focus="console.log('open-auto-focus')"
    @opened="console.log('opened')"
    @close="console.log('close')"
    @close-auto-focus="console.log('close-auto-focus')"
    @closed="console.log('closed')"
  >
    <span>It's a event Dialog</span>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="dialogVisible = false">
          Confirm
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const dialogVisible = ref(false)
</script>
```

## API

### Attributes

| 属性名 | 说明 | 类型 | 默认 |
| --- | --- | --- | --- |
| model-value / v-model | 是否显示 Dialog | `boolean` | false |
| title | Dialog 对话框 Dialog 的标题， 也可通过具名 slot （见下表）传入 | `string` | '' |
| width | 对话框的宽度，默认值为 50% | `string` / `number` | '' |
| fullscreen | 是否为全屏 Dialog | `boolean` | false |
| top | dialog CSS 中的 margin-top 值，默认为 15vh | `string` | '' |
| modal | 是否需要遮罩层 | `boolean` | true |
| modal-penetrable 2.10.5 | 是否允许穿透遮罩层。 modal 属性必须为 `false`。 | `boolean` | false |
| modal-class | 遮罩的自定义类名 | `string` | — |
| header-class 2.9.3 | header 部分的自定义 class 名 | `string` | — |
| body-class 2.9.3 | body 部分的自定义 class 名 | `string` | — |
| footer-class 2.9.3 | footer 部分的自定义 class 名 | `string` | — |
| append-to-body | Dialog 自身是否插入至 body 元素上。 嵌套的 Dialog 必须指定该属性并赋值为 `true` | `boolean` | false |
| append-to 2.4.3 | Dialog 挂载到哪个 DOM 元素 将覆盖 `append-to-body` | `CSSSelector` / `HTMLElement` | body |
| lock-scroll | 是否在 Dialog 出现时将 body 滚动锁定 | `boolean` | true |
| open-delay | dialog 打开的延时时间，单位毫秒 | `number` | 0 |
| close-delay | dialog 关闭的延时时间，单位毫秒 | `number` | 0 |
| close-on-click-modal | 是否可以通过点击 modal 关闭 Dialog | `boolean` | true |
| close-on-press-escape | 是否可以通过按下 ESC 关闭 Dialog | `boolean` | true |
| show-close | 是否显示关闭按钮 | `boolean` | true |
| before-close | 关闭前的回调，会暂停 Dialog 的关闭. 回调函数内执行 done 参数方法的时候才是真正关闭对话框的时候. | `Function` | — |
| draggable | 为 Dialog 启用可拖拽功能 | `boolean` | false |
| overflow 2.5.4 | 拖动范围可以超出可视区 | `boolean` | false |
| center | 是否让 Dialog 的 header 和 footer 部分居中排列 | `boolean` | false |
| align-center 2.2.16 | 是否水平垂直对齐对话框 | `boolean` | false |
| destroy-on-close | 当关闭 Dialog 时，销毁其中的元素 | `boolean` | false |
| close-icon | 自定义关闭图标，默认 Close | `string` / `Component` | — |
| z-index | 和原生的 CSS 的 z-index 相同，改变 z 轴的顺序 | `number` | — |
| header-aria-level a11y | header 的 `aria-level` 属性 | `string` | 2 |
| transition 2.10.5 | 对话框动画的自定义过渡配置。 可以是一个字符串（过渡名称），也可以是一个包含 Vue 过渡属性的对象。 | `string` / `object` | dialog-fade |
| custom-class deprecated | Dialog 的自定义类名 | `string` | '' |

WARNING

`custom-class` 已被 弃用, 之后将会在 [2.4.0]v-if 移除, 请使用 `class`.

### Slots

| 插槽名 | 说明 |
| --- | --- |
| default | 对话框的默认内容 |
| header | 对话框标题的内容；会替换标题部分，但不会移除关闭按钮。 |
| footer | Dialog 按钮操作区的内容 |
| title deprecated | 与 header 作用相同 请使用 header |

WARNING

`title` 已被弃用，并将在 3.0.0 版本中移除，请使用 `header` 代替。

### 事件

| 名称 | 详情 | Type |
| --- | --- | --- |
| open | Dialog 打开的回调 | `Function` |
| opened | Dialog 打开动画结束时的回调 | `Function` |
| close | Dialog 关闭的回调 | `Function` |
| closed | Dialog 关闭动画结束时的回调 | `Function` |
| open-auto-focus | 输入焦点聚焦在 Dialog 内容时的回调 | `Function` |
| close-auto-focus | 输入焦点从 Dialog 内容失焦时的回调 | `Function` |

### Exposes

| 名称 | 详情 | 类型 |
| --- | --- | --- |
| resetPosition 2.8.1 | 重置位置 | `Function` |
| handleClose 2.9.8 | 关闭对话框 | `Function` |

## FAQ

#### 在 SFC 文件中使用对话框，scope 样式不会生效

典型议题：#10515 (https://github.com/element-plus/element-plus/issues/10515)

PS：既然对话框是使用 `Teleport` 渲染的，建议在全局范围写入根节点的样式。

#### 当对话框被显示及隐藏时，页面元素会来回移动（抖动）

典型议题：#10481 (https://github.com/element-plus/element-plus/issues/10481)

PS：建议将滚动区域放置在一个挂载的 vue 节点，如 `<div id="app" />` 下，并对 body 使用 `overflow: hidden` 样式。

## 源代码

组件 (https://github.com/element-plus/element-plus/tree/dev/packages/components/dialog) • 样式 (https://github.com/element-plus/element-plus/tree/dev/packages/theme-chalk/src/dialog.scss) • 文档 (https://github.com/element-plus/element-plus/blob/dev/docs/en-US/component/dialog.md)

## 贡献者
