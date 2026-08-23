# 天气查询（weather-inquiry）

一个基于 Spring Boot 3 的天气查询 Web 应用：后端提供 REST 接口，前端为原生 HTML/CSS/JS 单页。数据来自 [Open-Meteo](https://open-meteo.com/) 免费 API，**无需申请 API Key**。

用户可通过城市名（支持中文 / 英文）或浏览器定位查询天气，获得实时天气与未来 7 天逐日预报。

---

## 功能特性

- **城市名查天气**：`GET /api/weather?city=城市名`，内部先做地理编码（城市名 → 经纬度），再查询实时 + 7 天预报。
- **经纬度直查**：`GET /api/weather/location?lat=..&lon=..`，配合浏览器定位直接按坐标查天气。
- **10 分钟内存缓存**：手写 `ConcurrentHashMap` + TTL 的本地缓存，命中时返回 `cached=true`，减轻上游 API 压力。
- **统一响应结构**：`WeatherResponse` 统一返回城市、国家、实时天气、逐日预报与缓存标记。
- **友好的错误处理**：空白城市名 400、城市不存在 404、上游 API 失败 503，统一返回 `{"error": "..."}`。
- **原生前端单页**：
  - WMO `weather_code` → 中文描述 + emoji 图标映射；
  - 摄氏度 / 华氏度、km/h / mph 单位切换（`localStorage` 记忆）；
  - 城市收藏（`localStorage` 持久化）；
  - 浏览器定位查询；
  - 缓存命中时显示“缓存”标签。

---

## 技术栈

| 项目 | 说明 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.4.13 |
| 构建工具 | Maven（含 Maven Wrapper `mvnw` / `mvnw.cmd`，wrapper 3.3.4，默认 Maven 3.9.16） |
| 依赖 | `spring-boot-starter-web`、`spring-boot-starter-test` |
| HTTP 客户端 | Spring 6 `RestClient`（`SimpleClientHttpRequestFactory`） |
| 前端 | 原生 HTML / CSS / JavaScript（无框架，静态资源位于 `src/main/resources/static`） |
| 数据来源 | Open-Meteo 免费 API（地理编码 + 天气预报） |

---

## 项目结构（概览）

```
weather-inquiry/
├── pom.xml                                  # Maven 配置
├── mvnw / mvnw.cmd                          # Maven Wrapper
├── .mvn/wrapper/maven-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/example/weather/
    │   │   ├── WeatherApplication.java      # 启动类
    │   │   ├── controller/
    │   │   │   └── WeatherController.java   # REST 控制器
    │   │   ├── service/
    │   │   │   └── WeatherService.java      # 业务逻辑 + 缓存
    │   │   ├── dto/                         # 数据传输对象（record）
    │   │   │   ├── WeatherResponse.java
    │   │   │   ├── CurrentWeather.java
    │   │   │   ├── DailyForecast.java
    │   │   │   ├── ForecastResponse.java
    │   │   │   ├── GeocodingResponse.java
    │   │   │   └── GeoResult.java
    │   │   └── exception/
    │   │       ├── CityNotFoundException.java
    │   │       ├── UpstreamApiException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       ├── application.properties       # 应用配置
    │       └── static/
    │           ├── index.html               # 页面结构
    │           ├── app.js                   # 前端逻辑
    │           └── style.css                # 样式
    └── test/java/com/example/weather/
        ├── WeatherApplicationTests.java
        ├── controller/WeatherControllerTest.java
        ├── service/WeatherServiceTest.java
        └── dto/DtoDeserializationTest.java
```

---

## 快速开始

### 环境要求

- JDK 17（或更高版本）
- Maven 3.6+（项目自带 Maven Wrapper，可在无本地 Maven 时直接使用 `mvnw`）
- 运行时可访问外网，用于调用 Open-Meteo API（构建与单元测试不需要外网）

### 构建运行

在项目根目录执行：

```bash
# Windows（PowerShell / CMD）
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

也可以先打包再运行：

```bash
# Windows
.\mvnw.cmd clean package
java -jar target\weather-0.0.1-SNAPSHOT.jar

# Linux / macOS
./mvnw clean package
java -jar target/weather-0.0.1-SNAPSHOT.jar
```

若本机已安装 Maven，可把 `mvnw` / `mvnw.cmd` 替换为 `mvn` 使用。

### 访问方式

启动成功后（`application.properties` 未配置端口，默认 **8080**），在浏览器打开：

```
http://localhost:8080/
```

前端会加载 `src/main/resources/static/index.html`，通过同源 `/api/weather`、`/api/weather/location` 接口查询天气。

---

## API 说明

所有接口均为 `GET`，返回 JSON（`application/json`）。

### 1. 按城市名查询天气

```
GET /api/weather?city={城市名}
```

- `city`（必填）：城市名，支持中文（如 `北京`）或英文（如 `Beijing`）。

**成功响应示例**（HTTP 200）：

```json
{
  "city": "北京",
  "country": "中国",
  "current": {
    "temperature": 28.5,
    "feelsLike": 30.1,
    "weatherCode": 0,
    "windSpeed": 12.0,
    "humidity": 55
  },
  "daily": [
    {
      "date": "2026-08-24",
      "weatherCode": 1,
      "tempMax": 30.2,
      "tempMin": 22.1,
      "precipProb": 10
    },
    {
      "date": "2026-08-25",
      "weatherCode": 61,
      "tempMax": 27.8,
      "tempMin": 21.5,
      "precipProb": 70
    }
  ],
  "cached": false
}
```

> 说明：`city` / `country` 直接来自 Open-Meteo 地理编码结果（地理编码请求使用 `language=zh`，故中文城市名会返回中文国家名）。字段名与 DTO record 的序列化字段一一对应。

### 2. 按经纬度查询天气（浏览器定位）

```
GET /api/weather/location?lat={纬度}&lon={经度}
```

- `lat`（必填）：纬度，`double`。
- `lon`（必填）：经度，`double`。

返回结构与 `/api/weather` 一致，其中 `city` 固定为 `"当前位置"`、`country` 为空字符串。

**示例**：

```
GET /api/weather/location?lat=30.2937&lon=120.1614
```

### 响应结构字段说明

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `city` | string | 城市名（定位查询时为“当前位置”） |
| `country` | string | 国家 / 地区名 |
| `current.temperature` | double | 当前温度（摄氏度） |
| `current.feelsLike` | double | 体感温度（摄氏度） |
| `current.weatherCode` | int | WMO 天气代码，前端据此映射图标与描述 |
| `current.windSpeed` | double | 风速（km/h） |
| `current.humidity` | int | 相对湿度（%） |
| `daily[].date` | string | 预报日期（`yyyy-MM-dd`） |
| `daily[].weatherCode` | int | 当日 WMO 天气代码 |
| `daily[].tempMax` | double | 当日最高温（摄氏度） |
| `daily[].tempMin` | double | 当日最低温（摄氏度） |
| `daily[].precipProb` | int | 当日最大降水概率（%） |
| `cached` | boolean | 是否命中内存缓存 |

### 错误码

统一由 `GlobalExceptionHandler` 处理，错误响应结构为 `{"error": "..."}`：

| HTTP 状态码 | 触发条件 | 响应示例 |
| --- | --- | --- |
| 400 Bad Request | 城市名为空 / 空白（`IllegalArgumentException`） | `{"error":"请输入城市名称"}` |
| 404 Not Found | 城市不存在（`CityNotFoundException`） | `{"error":"未找到城市: 火星"}` |
| 503 Service Unavailable | 上游 API 调用失败（`UpstreamApiException`，如网络异常、超时、上游 5xx/429 等） | `{"error":"上游天气服务暂不可用，请稍后重试"}` |

---

## 前端功能说明

前端为单页应用，文件位于 `src/main/resources/static/`：

- **`index.html`**：页面骨架，包含标题、单位切换按钮、搜索框、查询 / 定位按钮、收藏栏、错误提示区、实时天气卡片与逐日列表。
- **`style.css`**：响应式卡片式布局与配色样式。
- **`app.js`**：交互逻辑，包括：
  - **WMO 天气代码映射**：内置 `WEATHER_CODES` 表，将 `weather_code` 映射为 emoji 图标与中文描述（晴、多云、雨、雪、雷暴等）。
  - **单位切换**：`°C · km/h` ↔ `°F · mph`，选择状态保存在 `localStorage` 的 `unitMode` 中，刷新后仍保留。
  - **城市收藏**：收藏列表保存在 `localStorage` 的 `favoriteCities`，点击收藏标签可快速再次查询，点击 `×` 可移除；实时卡片上的 `☆/★` 用于收藏 / 取消收藏。
  - **浏览器定位**：`navigator.geolocation` 获取当前位置，坐标保留 4 位小数后调用 `/api/weather/location`。
  - **缓存标记**：当响应 `cached` 为 `true` 时，在标题旁显示“缓存”标签。
  - **错误展示**：接口返回非 2xx 时展示 `data.error`，网络异常时提示“无法连接服务器”。

---

## 测试运行

项目包含 4 个测试类（JUnit 5 + Mockito + `MockRestServiceServer`），覆盖控制器、服务层、DTO 反序列化与 Spring 上下文加载。测试不依赖真实网络。

在项目根目录执行：

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

测试类概览：

| 测试类 | 覆盖内容 |
| --- | --- |
| `WeatherControllerTest` | `@WebMvcTest` 验证接口路径、参数、响应字段与 400/404/503 错误映射 |
| `WeatherServiceTest` | URL 构建、结果提取、缓存命中 / 归一化 / 过期、列式 daily 转逐日、Mock 服务器下解析与异常包装 |
| `DtoDeserializationTest` | Jackson 反序列化地理编码与预报响应（含蛇形字段映射） |
| `WeatherApplicationTests` | `@SpringBootTest` 上下文加载 |

---

## 目录结构

```
weather-inquiry/
├── .gitattributes
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/example/weather/
    │   │   ├── WeatherApplication.java
    │   │   ├── controller/
    │   │   │   └── WeatherController.java
    │   │   ├── service/
    │   │   │   └── WeatherService.java
    │   │   ├── dto/
    │   │   │   ├── CurrentWeather.java
    │   │   │   ├── DailyForecast.java
    │   │   │   ├── ForecastResponse.java
    │   │   │   ├── GeocodingResponse.java
    │   │   │   ├── GeoResult.java
    │   │   │   └── WeatherResponse.java
    │   │   └── exception/
    │   │       ├── CityNotFoundException.java
    │   │       ├── GlobalExceptionHandler.java
    │   │       └── UpstreamApiException.java
    │   └── resources/
    │       ├── application.properties
    │       └── static/
    │           ├── app.js
    │           ├── index.html
    │           └── style.css
    └── test/
        └── java/com/example/weather/
            ├── WeatherApplicationTests.java
            ├── controller/
            │   └── WeatherControllerTest.java
            ├── dto/
            │   └── DtoDeserializationTest.java
            └── service/
                └── WeatherServiceTest.java
```

---

## 设计说明

### 数据来源：Open-Meteo（免费，无需 API Key）

项目使用两个 Open-Meteo 公共接口：

| 用途 | 地址 |
| --- | --- |
| 地理编码（城市名 → 经纬度） | `https://geocoding-api.open-meteo.com/v1/search` |
| 天气预报（实时 + 逐日） | `https://api.open-meteo.com/v1/forecast` |

具体参数：

- 地理编码：`name={城市名}&count=1&language=zh`，取第一条匹配结果，其中城市名经 UTF-8 URL 编码。
- 天气预报：`latitude`、`longitude`、`current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m`、`daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max`、`timezone=auto&forecast_days=7`。

上游的 `daily` 字段为“列式平行数组”（`time`、`weather_code`、`temperature_2m_max` 等分别存放），服务层按下标对齐转换为 `List<DailyForecast>`，对 `null` 情况做了安全处理。

### 缓存设计

- 使用 `ConcurrentHashMap<String, CacheEntry>` 存储组装好的最终响应，键为归一化后的城市名或 `loc:` 前缀的坐标字符串。
- TTL 为 **10 分钟**，采用**惰性淘汰**：仅在读取时判断是否过期，命中（未过期）返回 `cached=true` 的副本，不污染缓存中的原始对象。
- 城市名归一化为 `trim + 小写`（`Locale.ROOT`），`"Beijing"`、`"beijing"`、`" Beijing "` 命中同一缓存 key。
- 空白城市名在查缓存之前即被校验拦截，不会写入缓存。
- 城市名查询与经纬度定位使用不同前缀的缓存 key，互不干扰。

### 超时设计

`RestClient` 基于 `SimpleClientHttpRequestFactory` 配置 **连接超时 5 秒、读取超时 5 秒**，避免上游响应缓慢拖垮前端请求。

### 错误处理设计

| 场景 | 异常 | HTTP 状态 |
| --- | --- | --- |
| 城市名为空白 | `IllegalArgumentException` | 400 |
| 地理编码无结果 | `CityNotFoundException` | 404 |
| 调用上游失败（网络异常、超时、5xx 等） | `UpstreamApiException` | 503 |

所有异常由 `@RestControllerAdvice` 的 `GlobalExceptionHandler` 统一捕获，返回简洁的 `{"error": "..."}` 结构；其中上游异常对外统一返回友好提示，不暴露内部细节。

### 其他实现细节

- 中文城市名采用 `URLEncoder.encode(..., UTF_8)` 编码后，以 `URI` 形式传给 `RestClient`，避免按本机默认字符集（如 GBK）重新编码导致的乱码问题。
- 经纬度查询时构造 `GeoResult("当前位置", "", lat, lon)` 作为占位地理信息，跳过地理编码直接查询预报。