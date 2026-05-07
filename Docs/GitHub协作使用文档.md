# AI Digital Human Scenic Guide 项目 GitHub 协作使用文档

## 1. 文档目的

本文档用于统一本项目的 GitHub 使用方式，确保团队成员在并行开发时分工清晰、代码同步顺畅、版本可追踪、联调过程可控。

本项目为三人协作开发项目，采用 `main`、`dev`、`feature/*` 分支协作模式。

---

## 2. 仓库信息

GitHub 仓库地址：

[https://github.com/Jack-cyberB/AI-Digital-Human-Scenic-Guide](https://github.com/Jack-cyberB/AI-Digital-Human-Scenic-Guide)

本地项目目录：

`D:\Project\2026\AI Digital Human Scenic Guide`

---

## 3. 分支说明

本项目使用以下分支：

### 3.1 `main`

`main` 为主分支，表示当前最稳定、最适合展示和交付的版本。

用途：

1. 存放当前阶段可用的正式版本。
2. 用于录制演示视频、作品打包、提交比赛材料。
3. 不直接进行日常开发。

管理规则：

1. 非必要情况下不要直接在 `main` 上修改代码。
2. 只有当 `dev` 分支完成联调并验证稳定后，才合并到 `main`。

### 3.2 `dev`

`dev` 为开发集成分支，表示团队当前的公共开发版本。

用途：

1. 汇总各成员阶段性完成的功能。
2. 用于前后端联调、接口联调、整体测试。
3. 作为所有成员日常同步最新代码的主要来源。

管理规则：

1. 每位成员完成一项相对独立的功能后，再合并到 `dev`。
2. `dev` 分支允许存在少量未完全优化的功能，但应保证基本可运行。
3. 所有成员日常同步最新代码时，优先从 `dev` 获取。

### 3.3 `feature/A`

`feature/A` 为成员 A 的个人开发分支。

建议职责：

1. 后端服务开发。
2. AI 模块集成。
3. 知识库、RAG、语音识别、语音合成、数字人接口整合。

### 3.4 `feature/B`

`feature/B` 为成员 B 的个人开发分支。

建议职责：

1. 游客端 App 开发。
2. 语音交互页面。
3. 地图导览页面。
4. 景点详情与数字人展示联动。

### 3.5 `feature/C`

`feature/C` 为成员 C 的个人开发分支。

建议职责：

1. 管理后台开发。
2. 数据看板页面。
3. 知识库管理页面。
4. 问答质检与画像分析页面。

---

## 4. 协作原则

### 4.1 基本原则

1. 每个人只在自己的 `feature` 分支上进行日常开发。
2. 不直接在 `main` 上开发。
3. 团队公共联调统一在 `dev` 上进行。
4. 日常同步代码优先同步 `dev`，而不是互相直接合并彼此的功能分支。

### 4.2 合并原则

一个功能满足以下条件之一时，可以考虑合并到 `dev`：

1. 一个页面主体已完成。
2. 一个接口链路已跑通。
3. 一个模块已能参与联调。
4. 不会明显影响其他成员继续开发。

### 4.3 提交原则

1. 小步提交，避免长时间不提交。
2. 每次提交信息尽量能说明改动目的。
3. 不提交无关文件、临时文件、测试垃圾文件。

---

## 5. 推荐开发流程

本项目推荐的标准流程如下：

1. 从远程仓库拉取最新代码。
2. 切换到自己的 `feature` 分支。
3. 在个人分支上开发功能。
4. 开发过程中定期同步 `dev` 分支最新内容。
5. 功能完成后将个人分支合并到 `dev`。
6. 团队在 `dev` 上联调和测试。
7. 当 `dev` 稳定后，再合并到 `main`。

整体流向如下：

`feature/A` -> `dev`  
`feature/B` -> `dev`  
`feature/C` -> `dev`  
`dev` -> `main`

---

## 6. 首次使用步骤

### 6.1 克隆仓库

首次加入项目时，先克隆远程仓库：

```bash
git clone https://github.com/Jack-cyberB/AI-Digital-Human-Scenic-Guide.git
cd AI-Digital-Human-Scenic-Guide
```

### 6.2 查看远程分支

```bash
git branch -a
```

### 6.3 切换到自己的分支

成员 A：

```bash
git checkout feature/A
```

成员 B：

```bash
git checkout feature/B
```

成员 C：

```bash
git checkout feature/C
```

如果本地没有对应分支，可执行：

```bash
git checkout -b feature/A origin/feature/A
```

或：

```bash
git checkout -b feature/B origin/feature/B
```

或：

```bash
git checkout -b feature/C origin/feature/C
```

---

## 7. 日常开发命令

### 7.1 查看当前分支

```bash
git branch
```

### 7.2 查看当前改动

```bash
git status
```

### 7.3 提交代码

```bash
git add .
git commit -m "feat: 完成某功能"
```

示例：

```bash
git commit -m "feat: add scenic map page"
git commit -m "feat: integrate chat api"
git commit -m "fix: resolve dashboard chart issue"
```

### 7.4 推送到远程个人分支

成员 A：

```bash
git push origin feature/A
```

成员 B：

```bash
git push origin feature/B
```

成员 C：

```bash
git push origin feature/C
```

---

## 8. 如何同步团队最新代码

日常开发时，不建议直接频繁合并其他成员的 `feature` 分支，而应以 `dev` 作为公共同步来源。

### 8.1 拉取远程更新

```bash
git fetch origin
```

### 8.2 在个人分支中合并 `dev`

例如成员 B 在自己的分支中同步最新公共代码：

```bash
git checkout feature/B
git merge origin/dev
```

成员 A、C 同理，只需将分支名替换为自己的分支。

### 8.3 若出现冲突

处理流程：

1. 打开冲突文件。
2. 手动保留正确代码。
3. 删除冲突标记。
4. 重新执行提交。

完成后执行：

```bash
git add .
git commit -m "fix: resolve merge conflict with dev"
```

---

## 9. 如何将个人功能合并到 `dev`

### 9.1 先提交个人分支代码

```bash
git add .
git commit -m "feat: 完成功能开发"
git push origin feature/B
```

### 9.2 切换到 `dev`

```bash
git checkout dev
```

### 9.3 先同步远程 `dev`

```bash
git fetch origin
git merge origin/dev
```

### 9.4 合并个人分支

例如合并成员 B 的功能：

```bash
git merge feature/B
```

### 9.5 推送 `dev`

```bash
git push origin dev
```

---

## 10. 如何将 `dev` 合并到 `main`

当 `dev` 分支经过联调、测试，确认可作为阶段成果时，再执行：

```bash
git checkout main
git fetch origin
git merge origin/main
git merge dev
git push origin main
```

适用场景：

1. 准备录制演示视频。
2. 准备导出可执行版本。
3. 准备提交比赛作品。
4. 希望保留当前最稳定版本。

---

## 11. 建议提交信息规范

推荐使用以下前缀：

### 11.1 `feat`

表示新增功能。

示例：

```bash
feat: add scenic map page
feat: implement voice chat flow
```

### 11.2 `fix`

表示修复问题。

示例：

```bash
fix: resolve login api error
fix: correct route rendering bug
```

### 11.3 `docs`

表示文档修改。

示例：

```bash
docs: update project outline
docs: add github workflow guide
```

### 11.4 `refactor`

表示重构，不新增功能，也不修 bug。

示例：

```bash
refactor: simplify chat service structure
```

### 11.5 `chore`

表示杂项改动，如配置、初始化、依赖调整等。

示例：

```bash
chore: initialize repository
chore: update gitignore
```

---

## 12. 本项目建议模块边界

为了减少冲突，建议各成员主要维护以下目录或模块：

### 12.1 成员 A

负责：

1. 后端接口。
2. 数据库结构。
3. RAG 与知识库。
4. AI 服务接入。
5. 语音识别与语音合成。

### 12.2 成员 B

负责：

1. 移动端 App 页面。
2. 聊天交互页。
3. 地图导览页。
4. 景点点位联动页。
5. 数字人展示区域。

### 12.3 成员 C

负责：

1. 管理后台页面。
2. 数据看板。
3. 知识库管理。
4. 问答质检页面。
5. 用户画像与分析页面。

说明：

如确需跨模块修改，应提前在团队内说明，避免多人同时修改同一文件。

---

## 13. 常见问题

### 13.1 为什么不建议直接在 `main` 上开发

因为 `main` 代表稳定版本，直接开发容易引入未测试代码，影响作品交付质量。

### 13.2 为什么不建议经常直接合并别人的 `feature` 分支

因为这样会导致代码来源混乱，不利于统一集成和问题追踪。推荐统一以 `dev` 作为团队公共代码源。

### 13.3 如果我只想拿到别人还没合并到 `dev` 的代码怎么办

可以临时拉取对应分支，但只在必要时使用。例如：

```bash
git fetch origin feature/A
git merge origin/feature/A
```

这类操作只建议用于临时联调，不建议作为常规流程。

### 13.4 合并冲突很多怎么办

通常说明模块边界不清或长时间没有同步 `dev`。解决办法：

1. 更细分模块责任。
2. 提高同步频率。
3. 小步提交、小步合并。
4. 跨模块改动前先沟通。

---

## 14. 推荐工作节奏

建议团队采用以下节奏：

1. 每人每天至少提交一次本地进展。
2. 每完成一个小功能就推送远程备份。
3. 每 1 至 2 天同步一次 `dev`。
4. 每完成一个阶段后集中合并到 `dev` 联调。
5. 每周至少保留一个可运行版本到 `main`。

---

## 15. 总结

本项目 GitHub 协作的核心原则如下：

1. `feature/*` 用于个人开发。
2. `dev` 用于团队集成与联调。
3. `main` 用于稳定版本和最终交付。
4. 平时只在自己的分支开发。
5. 日常同步以 `dev` 为主，不频繁直接合并他人分支。
6. 保持小步提交、及时同步、及时联调。

按照以上规范执行，可以有效降低冲突风险，提高三人协作开发效率。
