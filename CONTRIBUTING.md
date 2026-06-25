# CONTRIBUTING.md — mes-query-api 编码规范

> 遵循阿里巴巴 Java 开发手册（泰山版），结合项目实际情况。

## 1. 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `TableController`, `TableQueryService` |
| 方法/变量 | lowerCamelCase | `listTables()`, `tableName` |
| 常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 包名 | 全小写，点分隔 | `com.mes.query.controller` |
| 枚举 | ENUM 类名以 Enum 结尾 | `ErrorCodeEnum` |

## 2. 分层规范

```
controller/   → 接收请求、参数校验、调用 Service、返回结果（不写业务逻辑）
service/      → 业务逻辑
mapper/       → 数据访问（本项目为只读，仅 SELECT）
config/       → 配置类
common/       → 公共类（统一返回体、异常、工具类）
vo/           → 视图对象（接口返回）
dto/          → 数据传输对象
```

## 3. 接口规范

### 3.1 统一返回体

所有接口必须使用 `Result<T>` 封装，禁止裸返 `Map`、`List`、`String`：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 3.2 异常处理

- 使用全局 `@RestControllerAdvice` 统一处理
- 禁止在 Controller 中 `try-catch` 后返回裸 `Map`
- 业务异常抛出自定义 `BusinessException`
- 参数校验失败抛出 `IllegalArgumentException`，由全局处理器捕获

### 3.3 参数校验

- Controller 入口做参数合法性校验（表名白名单、分页范围等）
- 不做 SQL 拼接——所有用户输入通过 `PreparedStatement` 参数化

## 4. 代码风格

### 4.1 POJO 用 Lombok

```java
@Data
public class TableInfoVO {
    private String tableName;
    private String tableComment;
}
```

禁止手写 getter/setter（除非有特殊逻辑）。

### 4.2 日志

```java
@Slf4j
public class SomeService {
    public void doSomething() {
        log.info("查询表: {}", tableName);
        log.error("查询失败", e);
    }
}
```

禁止 `System.out.println`。禁止在生产环境打印大对象（超过 500 字符截断）。

### 4.3 注释

- 所有 public 类、public 方法必须有 Javadoc
- 复杂业务逻辑写行内注释
- 代码即注释——命名清晰比注释多更好

```java
/**
 * 查询单表数据（分页 + 过滤）
 *
 * @param tableName 表名
 * @param page      页码，从 1 开始
 * @param size      每页条数，最大 500
 * @param filter    等值过滤条件，格式 "col1:val1,col2:val2"
 * @return 分页结果
 */
public Result<PageVO<Map<String, Object>>> queryTable(...)
```

### 4.4 禁止魔法值

```java
// ❌ 错误
if (size > 500) size = 500;

// ✅ 正确
private static final int MAX_PAGE_SIZE = 500;
if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
```

### 4.5 缩进与格式

- 4 空格缩进（不用 Tab）
- 单行不超过 120 字符
- IDE 统一 `.editorconfig`（后续补充）

## 5. SQL 安全

### 5.1 表名白名单

动态表名（`/tables/{tableName}`）必须校验目标表是否真实存在于 `ddcoreprd` 库中，防止恶意传参。

### 5.2 列名白名单

`filter` 参数中的列名必须通过 `information_schema.COLUMNS` 校验，确保列真实存在后才拼入 SQL。

### 5.3 参数化查询

禁止字符串拼接 SQL 值。所有值通过 `PreparedStatement.setXxx()` 传入。

```java
// ❌ 禁止
String sql = "SELECT * FROM " + tableName + " WHERE id = " + id;

// ✅ 正确
String sql = "SELECT * FROM " + validatedTable + " WHERE id = ?";
```

## 6. 依赖管理

### 6.1 版本统一

- Spring Boot 版本由 parent pom 管理
- 第三方依赖版本在 `<properties>` 中统一定义

### 6.2 排除无用依赖

本项目的 MyBatis-Plus 尚未实际使用（Query API 使用 JDBC 原生 API），后续如需使用 MyBatis-Plus 编写 Mapper，再引入。纯 JDBC 场景可移除此依赖以减小包体积。

## 7. 提交规范

### 7.1 Commit Message

```
<type>: <subject>

<body>
```

类型：
- `feat`: 新功能
- `fix`: 修复 bug
- `refactor`: 重构（不改变功能）
- `docs`: 文档
- `style`: 代码格式
- `chore`: 构建/工具变更

示例：`feat: 添加表结构查询接口`

### 7.2 分支策略

- `master`：稳定分支，保护
- `dev`：开发分支
- `feature/xxx`：功能分支

## 8. 参考

- [阿里巴巴 Java 开发手册（泰山版）](https://github.com/alibaba/p3c)
