# 分支管理策略

## 分支类型

| 分支 | 用途 | 命名 | 生命周期 |
|------|------|------|-----------|
| **master** | 主分支，稳定版本 | master | 永久 |
| **develop** | 开发分支 | develop | 永久 |
| **feature/** | 功能开发 | feature/功能名 | 临时 |
| **fix/** | Bug 修复 | fix/问题描述 | 临时 |
| **hotfix/** | 紧急修复 | hotfix/问题描述 | 临时 |
| **release/** | 发布准备 | release/版本号 | 临时 |

## 分支策略

### Git Flow

```
master ─────────────────────────────────────────────►
  ↑      ↑        ↑        ↑
  │      │        │        │
  │   release   │     hotfix
  │    v1.0     │       │
  │      │      │       │
  └──────┴──────┴───────┘
         ↑
         │
      develop ──────────────────────────────►
         ↑      ↑      ↑
         │      │      │
    feature/  feature/  fix/
    user     auth    login
```

## 分支命名规范

### 功能分支

```
feature/<issue-id>-<描述>
feature/USER-001-user-register
feature/AUTH-002-oauth-implement
```

### 修复分支

```
fix/<issue-id>-<描述>
fix/LOGIN-001-token-expire
```

### 热修复分支

```
hotfix/<issue-id>-<描述>
hotfix/SECURITY-001-password-leak
```

### 发布分支

```
release/v1.0.0
release/v1.1.0
```

## 工作流程

### 1. 开发新功能

```bash
# 1. 从 develop 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/USER-001-user-register

# 2. 开发并提交
git add .
git commit -m "feat(user): 添加用户注册功能"

# 3. 推送分支
git push origin feature/USER-001-user-register

# 4. 合并到 develop
git checkout develop
git merge feature/USER-001-user-register
git push origin develop

# 5. 删除功能分支
git branch -d feature/USER-001-user-register
```

### 2. Bug 修复

```bash
# 1. 从 develop 创建修复分支
git checkout develop
git checkout -b fix/LOGIN-001-token-issue

# 2. 修复并提交
git commit -m "fix(auth): 修复 Token 过期问题"

# 3. 合并
git checkout develop
git merge fix/LOGIN-001-token-issue
git push origin develop
```

### 3. 紧急修复

```bash
# 1. 从 master 创建热修复分支
git checkout master
git checkout -b hotfix/SECURITY-001-fix

# 2. 修复并提交
git commit -m "hotfix: 修复安全漏洞"

# 3. 合并到 master
git checkout master
git merge hotfix/SECURITY-001-fix
git tag -a v1.0.1 -m "安全修复版本"
git push origin master --tags

# 4. 合并到 develop
git checkout develop
git merge hotfix/SECURITY-001-fix
git push origin develop
```

## 版本管理

### 版本号格式

```
<major>.<minor>.<patch>[-<pre-release>]

示例：v1.0.0, v1.1.0, v2.0.0-beta
```

| 组成部分 | 说明 |
|----------|------|
| **major** | 不兼容的 API 变更 |
| **minor** | 向后兼容的新功能 |
| **patch** | 向后兼容的 Bug 修复 |
| **pre-release** | 预发布版本（alpha, beta, rc） |

### 发布流程

```bash
# 1. 创建发布分支
git checkout develop
git checkout -b release/v1.0.0

# 2. 版本号更新
# 修改 version 配置

# 3. 测试并修复

# 4. 合并到 master
git checkout master
git merge release/v1.0.0
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin master --tags

# 5. 合并回 develop
git checkout develop
git merge release/v1.0.0
git push origin develop
```

## 合并规则

### Pull Request 要求

- ✅ 必须有代码审查
- ✅ 必须通过 CI/CD 测试
- ✅ 必须更新文档（如有必要）
- ✅ 合并后删除源分支

### 冲突解决

```bash
# 1. 更新 develop
git checkout develop
git pull origin develop

# 2. 合并冲突分支
git checkout feature/xxx
git merge develop

# 3. 解决冲突后提交
git add .
git commit -m "merge: resolve conflict"
git push origin feature/xxx
```

## 保护分支

| 分支 | 保护规则 |
|------|----------|
| master | 必须 PR，禁止直接推送 |
| develop | 必须 PR，禁止直接推送 |

## 常用命令

```bash
# 查看分支
git branch -a

# 创建分支
git checkout -b feature/xxx

# 删除本地分支
git branch -d feature/xxx

# 删除远程分支
git push origin --delete feature/xxx

# 合并分支
git merge feature/xxx

# 变基（保持线性历史）
git rebase develop
```
