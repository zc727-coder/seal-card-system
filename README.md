# 印鉴卡管理系统

提供客户印鉴建档、多页影像、双人复核、账户适用范围、版本变更、停用、交易调阅和操作审计。

## 交付内容

离线部署包包含前端静态文件、Java 8后端JAR、MySQL/Nginx/应用镜像、建表SQL、配置模板和安装脚本。源码单独提供。目标平台为Linux AMD64容器；Windows需使用Docker Desktop的Linux容器模式。

## 安装

安装Docker Engine或Docker Desktop及Docker Compose V2或更高版本。运行程序无需另装JDK、Maven、Node或MySQL。

完整解压离线部署包，终端进入解压目录。

Windows：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/install.ps1
```

Linux：

```sh
sh scripts/install.sh
```

首次安装自动生成`.env`、独立随机密码及会话密钥，校验交付文件并导入镜像，随后启动数据库、3个后端实例和Nginx。已有`.env`不覆盖。重复运行同一安装命令即可启动已有环境。

访问地址：`http://127.0.0.1:8080`。API文档：`http://127.0.0.1:8080/doc.html`。

## 账号与使用

当前版本使用内置身份和客户中心适配器。B001机构账号：`maker01`录入、`checker01`复核、`teller01`交易、`auditor01`审计、`admin01`管理；B002账号为`maker02`、`checker02`。部署管理员从本机`.env`的`DEMO_PASSWORD`取得登录密码并单独分发。交付包不提供固定密码。

1. 录入人员新增档案，核验客户、选择账户、上传PNG/JPEG。多账户共用需填写授权依据。
2. 复核人员核对全部影像和适用范围后通过或驳回。录入人与复核人必须不同；可指定预约生效时间。
3. 影像或账户范围变更将提交新版本，审批前保持原生效版本。
4. 交易人员填写客户、付款账号、交易类型及用途后查询、调阅影像。票据历史查询需填写签发日期。
5. 审计人员按卡片、操作、结果或请求编号查询记录；管理员查看上传任务并重试失败任务。

内置客户：`C10001`（B001，`ACC00000123`、`ACC00000124`）、`C10002`（B001，`ACC00000223`）、`C20001`（B002，`ACC00000323`）。部署真实业务时需接入机构身份、客户及账户中心；本包不包含真实客户数据或电子验印功能。

## 配置

| 环境变量 | 用途 |
| --- | --- |
| HTTP_PORT | 访问端口，默认8080 |
| BIND_ADDRESS | 监听地址，默认127.0.0.1；需要远程访问时设置0.0.0.0 |
| MYSQL_ROOT_PASSWORD | 数据库管理密码 |
| RUNTIME_DB_PASSWORD | 应用数据库密码，32至128位十六进制 |
| DEMO_PASSWORD | 内置账号登录密码 |
| APP_AUTH_SECRET | 后端实例共用的会话签名密钥 |

修改`.env`后重新启动。已有数据库密码需同步修改MySQL账号，不能只修改环境变量。首次初始化自动执行`docs/sql/001_create_tables.sql`并创建最小权限账号`seal_runtime`；已有数据库不会重复建表。

影像最多20页，单页10MB。原图存放共享影像卷，通过鉴权接口读取。

## 运维

```sh
docker compose -f compose.release.yaml ps
docker compose -f compose.release.yaml logs --tail 100 backend nginx
docker compose -f compose.release.yaml stop
docker compose -f compose.release.yaml up -d --wait
```

部署配置固定3个后端副本，Nginx通过Docker DNS发现和分发请求。MySQL及后端不发布宿主端口。数据库和影像分别持久化在`mysql-data`、`image-data`卷中，实际卷名带Compose项目名前缀。日常停止保留卷，不使用`down -v`。

更新：先备份数据库与影像，再导入新镜像并运行启动命令；升级SQL需在维护窗口单独执行，不覆盖原数据库。备份时停止后端写入，同时导出MySQL和影像卷；恢复先在独立环境核验数据及文件摘要，再切换服务。

外网入口使用HTTPS反向代理。单机3个应用实例不包含数据库、入口及磁盘的跨主机容灾；这些设施需按实际运行环境配置。

## 源码构建

JDK 8、Maven 3.6.1、Node 24。

```sh
mvn -f backend/pom.xml -DskipTests package
cd frontend
npm ci
npm run build
```

后端输出`backend/target/seal-card-service.jar`，前端输出`frontend/dist`。容器构建定义位于`infra/runtime`。集成测试依赖独立MySQL测试库，普通构建可跳过环境依赖测试。
