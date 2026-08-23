# PR commit 对比报告

基准分支：`work`（`fe32778245713cec6dad979aae7e3bd77129958d`）。使用 `git fetch ... refs/pull/*/head` 获取 PR head，然后逐一执行 `git merge-base`、`git cherry` 与 `git merge-tree --write-tree`。

## 逐 PR 结果

| PR | Head SHA / 提交 | merge-base | git cherry | 分类 | 变更与备注 |
|---|---|---|---|---|---|
| #1 | `9aa795d` fix API 23 | `fe32778` | `+ 9aa795d` | 未包含 | `app/build.gradle.kts`, `MainActivity.kt` |
| #2 | `5202be6` 签名 release APK | `fe32778` | `+ 5202be6` | 未包含 | `.github/workflows/build.yml`, `app/build.gradle.kts`；与 #3 共享 workflow |
| #3 | `61604e4` Android checks/tests | `fe32778` | `+ 61604e4` | 未包含 | workflow、测试、`gradlew`；与 #2 共享 workflow |
| #4 | `606e066` 重命名 Application/启动测试 | `fe32778` | `+ 606e066` | 未包含 | 新增/重命名 Application 类及 instrumentation test |
| #5 | `36f9801` Nearby 状态线程安全 | `fe32778` | `+ 36f9801` | 未包含 | `NearbyChatService.kt`、测试 |
| #6 | `b6f0402` Nearby 停止时离线事件 | `fe32778` | `+ b6f0402` | 未包含 | `ChatRepository.kt`、`NearbyChatService.kt` |
| #7 | `cd50779` 失败消息重试 | `fe32778` | `+ cd50779` | 未包含 | `ChatRepository.kt`、`OutboundMessageQueue.kt` |
| #8 | `723ab1c` mesh participant 路由限制 | `fe32778` | `+ 723ab1c` | 未包含 | `ChatRepository.kt`、`NearbyChatService.kt` |
| #9 | `6b8d0e8` 恢复 UI instrumentation | `fe32778` | `+ 6b8d0e8` | 未包含 | 与 #3 共享 3 个测试文件 |
| #10 | `ba24d04` Nearby 生命周期前台服务化 | `fe32778` | `+ ba24d04` | 未包含 | `ChatRepository.kt` 及前台服务新文件 |
| #11 | `cfeefdc` 安全编码 endpoint metadata | `fe32778` | `+ cfeefdc` | 未包含 | `NearbyChatService.kt` |
| #12 | `29dba74` 持久化本地 member identity | `fe32778` | `+ 29dba74` | 未包含 | `ChatRepository.kt`、settings DAO/repository |

所有 PR 都以基准提交为直接 parent；因此没有重复提交或已包含提交（`git cherry` 全部为 `+`），也不存在“部分包含”。

## 冲突风险、重复与依赖顺序

- **直接合并检查**：对每个 PR 执行 `git merge-tree --write-tree work origin/pr/N` 均返回 0，单独合入基准时没有文本冲突。
- **高风险叠加组**：#5/#6/#8/#11 都修改 `NearbyChatService.kt`；#6/#7/#8/#10/#12 都修改 `ChatRepository.kt`。这些提交相互独立且都从同一 parent 分叉，按任意顺序连续 cherry-pick 可能出现上下文冲突或后者覆盖前者，建议先合并并运行测试，再逐个重放。
- **中风险叠加组**：#2/#3 修改同一 GitHub Actions workflow；#3/#9 修改相同 instrumentation/unit 测试文件。先整合 CI，再处理测试恢复，避免重复添加/删除测试步骤。
- **潜在语义依赖**：#10 引入前台服务生命周期，可能改变 #6 的停止事件及 #5 的并发状态路径；#12 改变 repository 的身份来源，需在 #7 的队列重试场景验证身份稳定性。#1 的 API 23 防护应优先于依赖其编译配置的其它 Android 变更。
- **建议顺序**：基础兼容性 #1 → CI/测试 #2 → #3 → #9 → 应用启动 #4 → Nearby 核心改动 #5 → #11 → #6 → #8 → repository/队列 #7 → 生命周期 #10 → 身份持久化 #12。每一步后运行 `git cherry` 和模块测试；若出现同文件冲突，以最新行为需求为准手工合并。
