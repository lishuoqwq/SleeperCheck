# “睡了吗” APP 技术设计文档 (Technical Design Document)

**Author:** YPrompt (Google Chief Android Engineer Persona)  
**Version:** 1.0  
**Date:** 2026-01-13  
**Status:** Ready for Development  

---

## 1. 概述 (Overview)

本设计文档旨在为“睡了吗”APP提供一套极致简洁、本地优先且低功耗的技术实现方案。应用的核心价值在于帮助用户记录和改善睡眠习惯，而非通过侵入式手段监控用户。所有数据存储、逻辑运算均在本地完成，利用安卓原生 API 实现“无感知”的数据记录。

设计哲学遵循：**Less is More (少即是多)**。我们不追求繁杂的后台保活，而是利用系统既有的统计数据，以最低的功耗代价换取最精准的习惯洞察。

---

<data_model>
## 2. 数据模型 (Data Model)

基于“本地优先”原则，我们使用 Android Jetpack Room (SQLite) 作为本地数据库。数据结构设计追求扁平化与原子性，确保读写高效。

### 2.1 每日记录 (`DailyRecord`)
核心实体，用于存储每日的打卡状态及熬夜判定结果。

```kotlin
@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey
    val date: String, // 格式: "YYYY-MM-DD", 作为主键确保每日唯一
    
    @ColumnInfo(name = "did_check_in")
    val didCheckIn: Boolean = false, // 用户是否主动打卡
    
    @ColumnInfo(name = "check_in_timestamp")
    val checkInTimestamp: Long? = null, // 打卡时的毫秒级时间戳
    
    @ColumnInfo(name = "is_stay_up_late")
    val isStayUpLate: Boolean = false, // 系统判定是否熬夜
    
    @ColumnInfo(name = "last_screen_off_time")
    val lastScreenOffTime: Long? = null, // 判定当晚最后一次熄屏/停止使用的时间
    
    @ColumnInfo(name = "note")
    val note: String? = null // 用户可选备注 (e.g., "加班", "失眠")
)
```

### 2.2 月度统计 (`MonthlyStat`)
用于快速渲染日历视图和趋势图的聚合表，每日打卡后异步更新。

```kotlin
@Entity(tableName = "monthly_stats")
data class MonthlyStat(
    @PrimaryKey
    val month: String, // 格式: "YYYY-MM"
    
    @ColumnInfo(name = "total_check_ins")
    val totalCheckIns: Int = 0,
    
    @ColumnInfo(name = "total_stay_up_late_days")
    val totalStayUpLateDays: Int = 0,
    
    @ColumnInfo(name = "max_consecutive_check_ins")
    val maxConsecutiveCheckIns: Int = 0, // 最长连续打卡天数
    
    @ColumnInfo(name = "status_bitmap")
    val statusBitmap: String // 以字符串形式存储当月每日状态的紧凑映射，用于快速热力图渲染
)
```

### 2.3 用户配置 (`UserPreferences`)
使用 DataStore (Proto DataStore) 存储单例配置，保证类型安全。

```kotlin
data class UserPreferences(
    val targetSleepTimeHour: Int = 23, // 目标睡觉时间 (小时)
    val targetSleepTimeMinute: Int = 0, // 目标睡觉时间 (分钟)
    val stayUpLateThresholdHour: Int = 1, // 熬夜判定阈值 (e.g., 凌晨1点后还在用手机)
    val isDailyReminderEnabled: Boolean = true,
    val reminderTimeHour: Int = 22,
    val reminderTimeMinute: Int = 30
)
```
</data_model>

---

<core_logic>
## 3. 核心功能逻辑 (Core Function Logic)

### 3.1 熬夜检测机制 (The "Invisible" Detection)
**设计难点**：如何在不运行高耗电后台服务的前提下，准确判断用户昨晚是否熬夜？
**解决方案**：**回顾式查询 (Retrospective Query)**。
我们不实时监听屏幕，而是在用户次日打开 App（或通过极低频的 WorkManager）时，查询系统的 `UsageStatsManager`。

*   **逻辑流程**：
    1.  用户打开 App 进行“今日打卡”或查看数据。
    2.  App 请求 `UsageStatsManager` 权限（用户隐私敏感，需引导授权）。
    3.  查询时间窗口：`昨晚目标睡觉时间` 到 `今日凌晨4:00`（可配置）。
    4.  分析 `UsageEvents`，查找类型为 `MOVE_TO_FOREGROUND` 或 `USER_INTERACTION` 的事件。
    5.  如果在此“禁止时段”内存在显著的交互活动（忽略偶发点亮屏幕），则标记 `DailyRecord.isStayUpLate = true`。

*   **优势**：
    *   **0 后台功耗**：完全不需要后台 Service 常驻。
    *   **精准**：基于系统级日志，无法作弊。
    *   **隐私**：数据只在本地读取和处理，分析结果存入本地数据库，原始 Usage 数据不存储、不上传。

### 3.2 智能打卡与补卡 (Smart Check-in)
*   **自动填充**：如果检测逻辑判断昨晚未熬夜，且到了次日早晨（如 6:00 AM 后）用户解锁了手机，可通过 `WorkManager` 发送一个“昨晚睡得不错”的静默通知。用户点击通知即可一键记录“未熬夜”。
*   **手动修正**：算法并非百分百完美，允许用户手动修正当天的熬夜状态，体现“用户自主性”。

### 3.3 趋势分析 (Local Analytics)
*   所有统计分析（如“连续早睡挑战”、“熬夜热力图”）均在本地通过 Room 的 SQL 查询聚合完成。
*   **算法优化**：对于月度/年度统计，采用增量更新策略。每当 `DailyRecord` 变更时，仅重新计算受影响月份的 `MonthlyStat`，避免全量扫描。
</core_logic>

---

<user_settings>
## 4. 用户设置实现 (User Settings Implementation)

用户设置是体现“以用户为中心”的关键。所有设置修改即时生效，并驱动核心逻辑的参数变化。

### 实现细节
1.  **目标设定**：
    *   UI 组件：双向滑块圆盘时钟 (Circular Slider)。
    *   逻辑：设置 `targetSleepTime` (目标入睡) 和 `stayUpLateThreshold` (熬夜红线)。
    *   *User Value*: 让用户直观看到“目标”与“红线”的时间差。

2.  **提醒管理**：
    *   **睡前提醒**：使用 `AlarmManager` 的 `setExactAndAllowWhileIdle` (需适配 Android 12+ 精确闹钟权限)，在目标时间前 30 分钟发送本地通知。
    *   **防打扰**：检测到用户正在“使用手机”（屏幕亮且解锁）时才发送提醒？不，为了省电，我们只发送标准通知。如果用户开启了系统级“勿扰模式”，我们尊重系统设置，不强制弹窗。

3.  **隐私开关**：
    *   提供“停止使用统计访问”的快捷开关。一旦关闭，App 降级为纯手动记录模式，不再读取系统 UsageStats，彻底消除隐私顾虑。
</user_settings>

---

<permissions>
## 5. 权限解决方案 (Permissions Strategy)

作为注重隐私的 App，我们仅申请实现核心功能所必须的最小权限集。

### 5.1 必须权限
*   `POST_NOTIFICATIONS`: (Android 13+) 用于发送睡前提醒和早晨打卡日报。

### 5.2 核心功能权限 (可选，强烈推荐)
*   `PACKAGE_USAGE_STATS` (需在设置中手动授权):
    *   **用途**：实现“回顾式熬夜检测”。
    *   **申请策略**：在用户首次进入“智能检测”页面时，展示清晰的解释弹窗（Rationale），说明“我们需要读取屏幕使用时间来判断您是否熬夜，数据仅保存在本地”，然后跳转至系统设置页。
    *   **降级处理**：如果用户拒绝，App 自动切换为“手动记录模式”，核心功能不受阻碍，仅自动化程度降低。

### 5.3 排除的权限 (Explicitly Excluded)
*   `INTERNET`: **完全移除**。App 的 AndroidManifest.xml 中不应包含网络权限。这是“隐私至上”的最强证明。
*   `BOOT_COMPLETED`: 仅用于重新注册 `AlarmManager` 的提醒，不做其他后台启动。
*   `FOREGROUND_SERVICE`: **不使用**。我们不需要实时后台监控，利用 `UsageStats` + `WorkManager` 足矣。
</permissions>

---

<self_check>
## 6. 首席工程师自检 (Self-Check)

以下是对本技术方案的批判性审查，以确保符合 Google 首席工程师的设计标准。

| 审查维度 | 检查项 | 结果 | 说明 |
| :--- | :--- | :--- | :--- |
| **本地优先** | 是否依赖网络？ | **否** | 移除 INTERNET 权限，纯本地数据库。 |
| **本地优先** | 数据是否离线可用？ | **是** | 所有逻辑均在端侧闭环。 |
| **低功耗** | 是否有常驻后台服务？ | **无** | 采用回顾式查询 (Pull) 而非实时监听 (Push)。 |
| **低功耗** | 轮询机制是否优化？ | **是** | 仅在 App 启动或极低频 Job 中查询 UsageStats。 |
| **隐私至上** | 敏感权限是否最小化？ | **是** | `UsageStats` 为可选权限，且提供降级方案。 |
| **隐私至上** | 数据是否出境？ | **否** | 物理隔绝，无法上传。 |
| **简洁性** | 架构是否过度设计？ | **否** | 使用标准的 Room + DataStore，无复杂分层。 |
| **用户价值** | 是否解决了核心痛点？ | **是** | 自动化记录减少了用户坚持习惯的阻力。 |

**结论**：本方案在技术可行性、隐私保护和功耗控制之间取得了最佳平衡，符合交付标准。
</self_check>