<img src="docs/banner.jpg" alt="QuickView" width="100%">

# QuickView

> 视角书签 Mod —— 把相机机位存成书签，一键切过去，一键切回来。

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=flat-square)
![Loader](https://img.shields.io/badge/Loader-Fabric-DBB69B?style=flat-square)
![Side](https://img.shields.io/badge/Side-Client--only-4C8DFF?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

---

## 简介

用 Freecam（灵魂出窍）类 Mod 找机位时，想回到之前看过的那一个视角，往往只能靠手动一点点飘回去。

QuickView 把「相机机位」变成可保存的书签：记录坐标 + 朝向 + 维度，按 `V` 打开菜单点一下，视角**瞬间**切过去；不需要平滑动画，也不需要来回找路。切换期间玩家本体留在原地不动，移动输入被拦截，可以自由飞行地勘察机位；再按一次即可恢复本体视角。

## 功能特性

- **视角书签**：保存相机坐标（X/Y/Z）+ 朝向（Yaw/Pitch）+ 维度，跨维度自动隔离
- **一键切换**：点击书签瞬间切到该机位，无过渡动画
- **一键恢复**：`B` 立刻回到玩家本体视角
- **本体不动**：切换期间拦截 WASD 移动输入，不会把角色带下悬崖
- **自由移动**：切过去之后可以自由飞行（WASD + 空格/Shift），带加速与减速的手感调节
- **自由视角下继续存点**：新增书签（含「设为当前」与快捷键快存）取的是**当前正在看的相机机位**，不是玩家本体的视角
- **跟随鼠标**：默认第一人称，按 `F5` 切第三人称时能看到并检查玩家本体
- **自由视角下隐藏手持物品**，避免手臂遮挡画面
- **可自定义按键**：设置页内置按键捕获按钮，支持重置为默认
- **拼音搜索**：书签列表支持中文、全拼、声母三种输入，且**多音字按词组上下文取音**
  （`家里蹲` 可搜 `jia` / `jialidun` / `jld`；`矿洞Boss房` 可搜 `kdbf`；`重庆` 可搜 `cq` / `chongqing`）
- **拖拽排序**：点「排序」按钮进入排序模式，按住条目拖动即可调整顺序（条目跟着光标走、落点有预览框），松手才落位并保存
- **分组**：给书签填一个分组名，主界面可按分组筛选（分组按钮循环切换）
- **可选平滑过渡**：切换视角时可以瞬间跳过去，也可以平滑飞过去（设置页开关）
- **分类存储**：多人服务器按服务器地址、单人按存档名，再按维度分文件

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.17.0 |
| Fabric API | 必需 |
| [LibGui](https://github.com/CottonMC/LibGui) | ≥ 15.1.0 |
| Java | 21+ |

纯客户端 Mod：**不需要**安装在服务端。

## 安装

1. 安装 Fabric Loader（1.21.11）
2. 把 [Fabric API](https://modrinth.com/mod/fabric-api) 与 [LibGui](https://github.com/CottonMC/LibGui) 放进 `mods/`
3. 把 `quickview-<版本>.jar` 放进 `mods/`
4. 启动游戏，按键设置在「选项 → 控制 → 按键绑定 → QuickView」

## 快速上手

1. 走到想记录的机位，按 `V` 打开菜单 → 点 `+` 新增书签（自动填入**当前视角**的坐标与朝向，可在编辑面板里微调或点「设为当前」重新取值）
2. 换个位置，再存几个书签
3. 之后任意时刻按 `V`，点书签即可**瞬间**切到该机位
4. 按 `B` 恢复本体视角

想边看边挪？切到书签后按 `G` 打开自由移动，用 `WASD` + `空格/Shift` 飞行，鼠标控制朝向；此时再按 `V` → `+` 或 `N` 存点，记下的就是你现在看到的位置。

## 默认按键

| 按键 | 功能 |
|---|---|
| `V` | 打开 QuickView 菜单 |
| `B` | 恢复本体视角 |
| `N` | 快速保存当前视角为书签（`View N`） |
| `G` | 切换自由移动（自由视角下生效，游戏内会提示开关状态） |
| `H` | 切换「灵魂出窍优先」 |

全部可在设置页（主菜单右下角 ⚙）里改键，`重置` 按钮可恢复默认。

## 界面说明

- **主菜单**：搜索框 + 书签列表 + `+` / 编辑 / 删除 / 恢复视角 / 排序 / 分组 / 设置
  - 搜索框支持中文原文、全拼、声母（不区分大小写，自动忽略首尾空格）。多音字由词级分词处理
  - **排序模式**（`排序` 按钮，与编辑/删除互斥）：开启后按住条目拖动可调整顺序 ——
    源条目变暗、半透明条目跟着光标走、目标格子显示落点预览框，**松手才真正落位并写入存档**；
    排序模式下单击条目不做任何事，避免误切换视角
  - 分组按钮循环切换筛选：`全部` → 各分组 → 回到 `全部`；没有分组时该按钮置灰
  - 打开「编辑」开关后点书签条目进入编辑面板；打开「删除」开关后点条目会**先弹二次确认**，确认后才删除
- **编辑面板**：名称、分组（可选）、X / Y / Z / Yaw / Pitch（仅允许数值），右下角「设为当前」把当前相机机位写入该书签
- **设置页**：双 Tab
  - 功能：快速添加、自由移动、灵魂出窍优先、切换视角平滑过渡
  - 快捷键：改键 + 重置

## 设置项含义

| 设置 | 说明 |
|---|---|
| 快速添加 | 是否允许用 `N` 快速保存（默认开） |
| 自由移动 | 切换视角后是否允许自由飞行（默认开，可用 `G` 随时切换） |
| 灵魂出窍优先 | 与其他 Freecam 类 Mod 共存时，优先以「相机实体」而不是玩家本体取景（默认开） |
| 切换视角平滑过渡 | 点书签时是否从当前位置飞过去而不是瞬间跳过去（默认关；过渡期间鼠标不接管视角） |

> 这几项现在会持久化到 `.minecraft/config/quickview.json`，改了就存，重启后保持。

## 数据存储

书签以 JSON 保存在游戏目录下，按「上下文 / 维度」分文件：

```
.minecraft/quickview/<服务器地址或存档名>/<维度>.json
```

文件内容是带版本号的对象，便于以后升级格式：

```json
{ "version": 1, "viewpoints": [ { "name": "Home", "dimension": "minecraft:overworld", "x": 0.0, ... } ] }
```

（1.0.1 及更早版本的裸数组格式仍可正常读取，保存时会自动升级。）

- 多人服务器：用服务器地址作为上下文，切换服务器不会串数据
- 单人存档：用存档名作为上下文
- 维度：`minecraft:overworld` / `minecraft:the_nether` / `minecraft:the_end` 等各存一份
- 写入采用「临时文件 + 原子替换」，中途崩溃不会损坏已有书签

直接删除对应 JSON 即可清空该书签列表。

设置项另存在 `.minecraft/config/quickview.json`（同样是带 `version` 的对象，缺字段时按默认值处理）。

## 从源码构建

需要 JDK 21。

```powershell
.\gradlew build
```

产物在 `build/libs/`，命名随 git 分支自动切换：

| 构建所在分支 | 产物 |
|---|---|
| `dev` | `quickview-<版本>-dev.jar` |
| `main` / tag | `quickview-<版本>.jar` |
| 任意分支 | `gradlew build -Prelease` 强制使用正式版本号 |

## 项目结构

```
src/main/java/dev/quickview/
├── QuickViewClient.java        # 客户端入口
├── QuickViewManager.java       # 核心状态机：切视角、自由移动、书签增删改、拖拽排序落盘
├── QuickViewConfig.java        # 全局设置（config/quickview.json）
├── QuickViewKeybindings.java   # 按键注册与轮询
├── Viewpoint.java              # 书签数据模型（含分组）
├── ViewpointStorage.java       # Gson JSON 存取（按上下文/维度分文件）
├── JsonFile.java               # JSON 落盘：临时文件 + 原子替换
├── PinyinSearch.java           # 搜索匹配键：原文/全拼/声母（内置 houbb/pinyin）
├── gui/                        # LibGui 界面（主菜单、编辑面板、设置页、确认框、滚动列表、改键按钮、拖拽条目）
└── mixin/
    ├── CameraMixin.java            # 视角激活时覆盖相机 pos/rotation
    ├── KeyboardInputMixin.java     # 拦截本体移动输入，转给自由相机
    ├── EntityMixin.java            # 把鼠标视角变化转给自由相机
    └── HeldItemRendererMixin.java  # 隐藏手持物品
```

## 分支与版本

- `main`：只放正式发布版本，first-parent 链即发布线
- `dev`：日常开发分支，功能与修复都提交在这里
- 发布时：`main` 合并 `dev` → 在 `main` 上升版本号并打 tag → 构建正式 jar → 版本号改动合并回 `dev`

## 路线图

- [x] 设置项持久化（`config/quickview.json`，改动即存）
- [x] 书签列表拖拽排序 / 分组
- [x] 切换视角时的可选平滑过渡

## 注意事项

自由视角类功能在部分多人服务器可能被规则禁止，请在服务器游玩前自行确认；由此产生的后果与本 Mod 无关。

## 许可

本项目采用 MIT 许可（`fabric.mod.json` 中的 `license` 字段为 `MIT`）。

### 内置的第三方库

以下库通过 Loom 的 `include` 打进 mod jar（jar-in-jar，位于 `META-INF/jars/`），**无需用户单独安装**：

| 库 | 版本 | 许可 | 用途 |
|---|---|---|---|
| [houbb/pinyin](https://github.com/houbb/pinyin) | 0.4.0 | Apache License 2.0 | 汉字转拼音、多音字词级消歧 |
| [houbb/heaven](https://github.com/houbb/heaven) | 0.2.0 | Apache License 2.0 | houbb/pinyin 的运行时依赖 |
| [houbb/nlp-common](https://github.com/houbb/nlp-common) | 0.0.5 | Apache License 2.0 | 分词用的 trie/dfa，houbb/pinyin 依赖 |
