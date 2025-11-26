# ✨ 增强宠物 — 增强宠物系统 ✨

![Version](https://img.shields.io/badge/Version-v1.1.0-blue.svg)
![Compatibility](https://img.shields.io/badge/MC%20Version-1.17%E2%80%941.21.x-orange.svg)
![Java](https://img.shields.io/badge/Java-17%2B-RED)
![Discord Support](https://img.shields.io/discord/b7BVkJ56mR?label=Discord&logo=discord&color=7289DA)

### 原作者已经允许Fork自行构建Release，但自行构建本项目代码仓库而产生的插件与本项目无关.
依据：https://discord.com/channels/1343274030169456701/1343274030651936843/1442903488723030056
### The original author has allowed Fork to build the Release independently, but any plugins generated from building this project's code repository are not related to this project.
> 无需复杂操作即可提升原版宠物体验！
> 更智能、更友好、更易管理的狼、猫、鹦鹉等宠物。

鸣谢 (Thanks to):
- cystol
- AxoIsAxo
- Nils Gereke
- All contributor who working in origin code.
---

## 📚 概述
EnhancedPets让原版宠物真正实用且易于管理。玩家获得简洁GUI、快捷操作和批量工具；管理员获得简易配置、自动保存和稳定存储。

- 玩家友好：直观菜单，Shift+双击右键打开宠物GUI，安全确认
- 管理便捷：按玩家分列JSON存储，自动保存，快速重载配置，旧版配置迁移
- 开发人员持续为您提供周期性冷笑话服务（请使用问题反馈区）

---
## 💡 核心功能
- 💎 宠物模式
  - 被动、中立、攻击性——可从GUI即时切换
  - *又名"禅境"、"默认模式"和"让你飞起来"*

- 🧭 传送与安抚
  - 召唤任意宠物至身边
  - 一键清除目标/仇恨值

- 🏷️ 重命名（含验证）
  - 通过聊天框重命名（限A-Z,0-9,_和-），或重置为默认名称
  - *2025革新：直呼其名不啰嗦*

- ⭐ 收藏功能
  - 标记重要宠物——收藏项置顶显示
  - *建议全选收藏，否则铲屎官纯度有待商榷（狗头*

- 🪑 坐/站指令
  - 切换可坐宠物状态（狼、猫）
  - 为用户精心设计的微操功能

- 🌱 幼崽成长控制
  - 暂停幼崽成长（单宠/批量）；受保护任务监管

- 👥 友善玩家（白名单）
  - 添加宠物永不攻击的玩家；支持单宠或批量管理
  - 团结就是力量

- 🤝 互不侵犯条约
  - 启用后宠物不会攻击玩家（即使处于攻击模式）
  - 相应玩家也无法伤害该宠物（通过直接攻击）
  - 和平诚可贵！请慎重使用该功能...除非要组建狗狗军团（发来！）

- 🎨 宠物显示自定义（新功能！）
  - 自定义图标： 将手中物品设为GUI中宠物显示图标！（Shift+单击重置）
  - 名称颜色： 为每只宠物选择独特的ChatColor显示名称！（Shift+单击重置）

- 🧺 批量操作
  - 选择类型（如狼）→勾选宠物→批量执行：
  - 设置模式、收藏开关、坐/站、传送、安抚、管理友方、转移或释放

- 🪦 死亡宠物流程
  - 宠物死亡后以骷髅头形式保留在GUI中
  - 使用下界之星复活（恢复项包括项圈颜色、变种、生命值等元数据），或永久删除

- 🧭 扫描与同步
  - 扫描已加载区块，找回未被管理的已驯服宠物

- 💾 快速存储
  - 按玩家分列JSON文件；每2分钟自动保存；支持从旧版config.yml一次性迁移

- 🔁 安全重载
  - /pets reload可更新配置并透明重启内部任务

- 🐙 快乐恶魂（1.21.6+）
  - 若服务器为1.21.6+：
  - 用雪球右击驯服（每次20%成功率）
  - 通过相同GUI和批量工具管理
  - 骑乘时左键发射火球（有冷却）
  - 请勿虐待您的快乐恶魂
  - 攻击模式须知：处于攻击模式的宠物会主动寻找可视范围内非友善且非你所有的有效目标，不仅限于"敌对生物"。
  - 其他注意事项：被动攻击模式正在开发中。

- 🖼️ 界面与体验
  - 精心设计兼顾美学与实用性（真实不虚）
  - 主菜单按收藏→类型→名称/ID排序显示所有宠物
  - 单体管理界面含快捷操作与安全确认
  - 批量菜单支持类型筛选、宠物勾选和群组管理
  - （图片即将上线！）（或许还有GIF）

## ⚙️ 安装指南
  - 预构建版本
    - 下载最新enhancedpets.jar
    - 放入plugins/文件夹
    - 重启服务器

## 源码构建（Maven）

  - git clone https://github.com/AxoIsAxo/EnhancedPets.git
  - cd EnhancedPets
  - mvn clean install
  - 复制target/enhancedpets-xxx.jar到plugins/
  - 重启服务器

- 需求:
  - Java 17+
  - Spigot/Paper 1.17–1.21.x（部分新功能如狼变种需1.21+）

## ⌨️ 指令与权限
指令

/pets —— 打开宠物GUI

/pets reload —— 重载插件配置


权限

enhancedpets.use —— 使用/pets和GUI（默认：true）

enhancedpets.reload —— 允许/pets reload（默认：op）

enhancedpets.admin —— 预留管理功能（默认：op）

快捷操作
Shift+双击右键已驯服宠物可立即打开其GUI（可配置）

🛠️ 配置
默认config.yml

```YML

# EnhancedPets配置

# 猫是否主动攻击附近敌对生物？
cats-attack-hostiles: false

# 狗对苦力怕的反应？
# NEUTRAL: 原版行为（无视除非主人被苦力怕攻击）
# ATTACK: 狗会主动攻击苦力怕
# FLEE: 狗会逃离附近苦力怕且不攻击
dog-creeper-behavior: NEUTRAL # 可选NEUTRAL, ATTACK, FLEE

# 是否启用1.14前豹猫驯服机制？
# 若开启，用生鳕鱼/鲑鱼右击未驯服成年豹猫可转化为家猫
ocelot-taming-legacy-style: false # 默认false保持原版特性

# 新增：Shift+双击右键打开所属宠物GUI？
# 若开启，250毫秒内双击自己宠物可打开GUI
shift-doubleclick-pet-gui: true

# 是否允许骑乘"欢乐幽灵"发射火球？
# 若开启，骑乘时左键可发射小火球
happy-ghast-fireball: true

# 需要高级日志？排查bug？
# 开启后控制台将输出详细插件信息
debug: false

#配置结束
```
备注

/pets reload只重载配置（宠物数据存储在JSON中不受影响）

启用时会自动迁移旧版config.yml中的"pet-data"到playerdata/*.json（一次性）

- 🔍 数据与自动保存
  - 经严格测试确保数据只在无人察觉时丢失（玩笑，其实很可靠）
  - 每位玩家的宠物数据存储在plugins/EnhancedPets/playerdata/<玩家UUID>.json
  - 自动保存每2分钟异步执行一次
  - 变更时也会按玩家进行同步保存（快速、安全、分组）

- 🧪 复活时恢复哪些属性？
  - 当然是一切可见之物，宠物将以昔日荣光重现。
  - 当死亡宠物复活时，EnhancedPets会恢复所有可能项！：

- ⚠️ 已知事项
  - 攻击模式会选择可视范围内非友善非你所有的有效目标，不限于怪物
  - 配置项"ocelot-taming-legacy-style"为未来扩展保留

- 🤝 支持
  - 疑问/建议/求助？加入Discord：https://discord.gg/b7BVkJ56mR （我们需要你！）

- 🙌 致谢
  - Code & design: cystol, AxoIsAxo
  - Community feedback and testing: You 💙


给生活增添宠物管理 — 比原版更可靠.




