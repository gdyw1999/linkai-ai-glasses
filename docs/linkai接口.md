# 记忆对话接口
版本要求： 标准版 及以上

## 介绍
该接口为 带上下文记忆 的对话API，相比普通对话接口，用户无需自行维护上下文记忆并通过messages参数传递，只需传入 用户问题和身份标识 即可，由系统按用户维度对上下文记忆进行维护。

## 支持的能力如下：

支持通过绑定 应用 或 工作流，使用其背后的 知识库 和 插件 等能力
支持根据应用或工作流中的 记忆轮次和时间 的配置对用户维度的记忆进行维护
支持一键切换所有支持的 大模型
支持 流式/非流式 输出
支持 多模态输入/输出，可输入文字；输出文字、图片、视频、文件
在线接口调试：API接口调试

## 接口定义
接口地址
POST https://api.link-ai.tech/v1/chat/memory/completions

## 请求头
参数	取值	说明
Authorization	Bearer YOUR_API_KEY	参考 接口鉴权说明 创建 API Key 并填入
Content-Type	application/json	表明使用JSON格式请求
请求体
参数	类型	是否必传	说明
question	string	是	用户本轮输入的问题
session_id	string	否	会话ID，每个session_id都有独立的上下文记忆存储。可传入对用户身份的标识，若该字段不填则系统会自动分配一个唯一标识，在响应中返回
app_code	string	否	应用或工作流的 code。若不填则表示不绑定具体应用，将请求直接传递给模型
model	string	否	模型编码，不传则使用应用的默认模型。所有可选模型见 模型列表
temperature	float	否	温度，默认为应用中配置的温度。取值范围为 [0, 1]，温度越高回复越具有创意和不确定性，温度越低则回复更严谨
top_p	int	否	控制模型采样范围，默认值为 1
frequency_penalty	float	否	频率惩罚项，该参数越大则更倾向于产生不同的内容，范围为 [-2, 2]，默认值为 0
presence_penalty	float	否	存在惩罚项，该参数越大则更倾向于产生不同的内容，范围为 [-2, 2]，默认值为 0
stream	bool	否	是否流式输出，默认值为 false
image_url	string	否	图片url地址，需要进行图像识别时可传入，支持jpg、jpeg、png格式
注意：

当通过 app_code 参数指定了应用时，可在应用管理页面中对 记忆轮次和保留时间 进行配置，同时会话记忆将按照 应用+会话ID 维度进行隔离存储，即同一个 session_id会在不同应用中有独立的上下文记忆：


当通过 app_code 参数指定了工作流时，系统将维护整个工作流的输入/输出记忆，可在大模型或应用节点中开启记忆并指定记忆的轮次：


请求示例:

{
    "app_code": "G7z6vKwp",
    "query": "你好",
    "session_id": "123e4567-e89b-12d3-a456-426614174000"
}

注意：

app_code：需换成你自己创建应用的code、应用广场中公开应用的code
session_id：一般为对用户身份的唯一标识，例如可将业务系统中用户ID、手机号等信息加密后传入。若该字段不传，系统将自动生成一个唯一ID并在响应中返回，下次对话时可携带该字段。
响应结果
非流式响应
接口调用默认为非流式响应，会在所有内容生成完毕后一次性返回：

{
    "session_id": "123e4567-e89b-12d3-a456-426614174000",
    "choices": [
        {
            "index": 0,
            "message": {
                "role": "assistant",
                "content": "你好，请问有什么可以帮助您的吗？"
            }
        }
    ],
    "usage": {
        "prompt_tokens": 9,
        "completion_tokens": 17,
        "total_tokens": 26
    }
}

注意：

session_id 为会话ID，如果调用时指定了该字段，将会原样返回，如果未指定则会自动分配一个唯一ID，下次调用该接口时可以携带。

choices.message.content 中为AI的响应内容，usage 部分 prompt_tokens、completion_tokens、total_tokens 分别表示请求的token数、响应的token数、全部消耗的token数，这三个费用相关字段直接使用模型返回的结果，平台不做处理。

一次对话的token计算包含 请求 和 响应 中的总token数， 其中请求又包含 应用设定、历史对话、知识库内容、用户问题，这些几个部分的token长度限制都可以在 应用管理 中找到。

### 流式响应
流式调用需要将传入参数 stream 设置为 true，将会在模型不断生成内容的过程中实时返回，适用于网页、APP、小程序等调用端进行流式输出：

data: {"choices": [{"index": 0, "delta": {"content": "你好！"}, "finish_reason": null}], "session_id": "123e4567-e89b-12d3-a456-426614174000"}

data: {"choices": [{"index": 0, "delta": {"content": "我能"}, "finish_reason": null}], "session_id": "123e4567-e89b-12d3-a456-426614174000"}

data: {"choices": [{"index": 0, "delta": {"content": "为你"}, "finish_reason": null}], "session_id": "123e4567-e89b-12d3-a456-426614174000"}

data: {"choices": [{"index": 0, "delta": {"content": "做些什么？"}, "finish_reason": null}], "session_id": "123e4567-e89b-12d3-a456-426614174000"}

data: {"choices": [{"index": 0, "delta": {}, "finish_reason": "stop", "usage": {"prompt_tokens": 9, "completion_tokens": 6, "total_tokens": 15}}], "session_id": "123e4567-e89b-12d3-a456-426614174000"}

data: [DONE]


注意：当输出为 "[DONE]" 时表示输出结束，其中每一行数据都会携带 session_id 字段。

## 错误说明
当接口异常时会返回以下结构：

{
    "error": {
        "message": "Invalid request: user message content is empty",
        "type": "invalid_request_error"
    }
}

根据 HTTP状态码 (status code) 和错误信息 判断错误类型：

HTTP状态码	描述
400	请求格式错误
401	接口鉴权失败，请检查 API Key 是否填写正确
402	应用不存在，请检查 app_code 参数是否正确
403	无访问权限，对于未公开应用，只有创建者账号才能调用
406	账号积分额度不足
408	无API访问权限，该API支持标准版及以上版本调用
409	内容审核不通过，问题、回答、检索的知识库中可能存在敏感词
503	接口调用异常，联系客服处理
## 示例代码
文本对话
### 1.CURL请求
非流式输出
流式输出
curl --request POST \
  --url https://api.link-ai.tech/v1/chat/memory/completions \
  --header 'Authorization: Bearer YOUR_API_KEY' \
  --header 'Content-Type: application/json' \
  --data '{
    "app_code": "",
    "question": "你是谁",
    "session_id": "123e4567-e89b-12d3-a456-426614174000"
  }'

注意：在 YOUR_API_KEY 处填入你创建的 API Key，在 app_code 中填入你创建的应用code。

### 2.Python代码请求
非流式输出
流式输出
import requests

url = "https://api.link-ai.tech/v1/chat/memory/completions"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer YOUR_API_KEY"
}
body = {
    "app_code": "",
    "question": "你好"
}
res = requests.post(url, json=body, headers=headers)
if res.status_code == 200:
    res_json = res.json()
    reply_text = res_json.get("choices")[0]['message']['content']
    session_id = res_json.get("session_id")
    print(f"session_id={session_id}, reply={reply_text}")
else:
    error = res.json().get("error")
    print(f"请求异常, 错误码={res.status_code}, 错误类型={error.get('type')}, 错误信息={error.get('message')}")


注意：在 YOUR_API_KEY 处填入你创建的 API Key，在 app_code 中填入你创建的应用code。

### 3.更多语言和在线调试
其他编程语言的接入代码可以在 API调试页面 进行代码生成，同时支持在线进行接口调试。

---

# 语音合成接口
版本要求： 标准版 及以上

## 介绍
语音合成接口，可将文本内容合成为语音文件，支持多种音色可供选择，覆盖通用问答、智能助手、有声阅读、视频配音、各地方言等场景。

在线接口调试：API接口调试

## 接口定义
接口地址
POST https://api.link-ai.tech/v1/audio/speech

## 请求头
| 参数 | 取值 | 说明 |
| --- | --- | --- |
| Authorization | Bearer YOUR_API_KEY | 参考 接口鉴权说明 创建 API Key 并填入 |
| Content-Type | application/json | 表明使用JSON格式请求 |

## 请求体
| 参数 | 类型 | 是否必传 | 说明 |
| --- | --- | --- | --- |
| input | string | 是 | 需要合成为语音的文本内容 |
| app_code | string | 否 | 应用code，如果填写了该值，则使用应用中设置的音色 |
| voice | string | 否 | 音色编码，所有可选声音见 音色列表 |

注意：
- `app_code` 和 `voice` 参数至少有一个不为空
- 如果填写了 `app_code` 则使用应用中设置的音色，无需填写voice参数
- 如果填写了 `voice` 参数则直接使用指定的音色，无需再设置app_code参数

在 **"应用 - 模型设置 - 声音"** 中可配置应用绑定的音色。

## 响应结果
响应以二进制流的形式进行输出，可将响应结果保存为文件后进行播放。

## 错误说明
当接口异常时会返回以下结构：

```
{
    "success": false,
    "code": 408,
    "message": "当前版本无该API访问权限",
    "data": null
}
```

根据 HTTP状态码 (status code) 和错误信息 判断错误类型：

| HTTP状态码 | 描述 |
| --- | --- |
| 400 | 请求格式错误 |
| 401 | 接口鉴权失败，请检查 API Key 是否填写正确 |
| 402 | 应用不存在，请检查 app_code 参数是否正确 |
| 403 | 无访问权限，对于未公开应用，只有创建者账号才能调用 |
| 406 | 账号积分额度不足 |
| 408 | 无API访问权限，该API支持标准版及以上版本调用 |
| 409 | 内容审核不通过，问题、回答、检索的知识库中可能存在敏感词 |
| 503 | 接口调用异常，联系客服处理 |

## 示例代码
### 1.CURL请求
```
curl https://api.link-ai.tech/v1/audio/speech \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "input": "你好，请问有什么需要帮助的?",
    "voice": "BV700_V2_streaming"
  }' \
  --output speech.mp3
```

注意：在 YOUR_API_KEY 处填入你创建的 API Key。

### 2.Python代码请求
```
import requests

url = 'https://api.link-ai.tech/v1/audio/speech'
headers = {
    'Authorization': 'Bearer YOUR_API_KEY',
    'Content-Type': 'application/json'
}
data = {
    'input': '你好，请问有什么需要帮助的?',
    'voice': 'BV007_streaming'
}
res = requests.post(url, headers=headers, json=data)
file_path = "speech.mp3"
if res.status_code == 200:
    with open(file_path, 'wb') as f:
        f.write(res.content)
    print(f"语音合成成功，语音文件：{file_path}")
else:
    error = res.json()
    print(f"请求异常, 错误码={error.get('code')}, 错误信息={error.get('message')}")
```

注意：在 YOUR_API_KEY 处填入你创建的 API Key。

### 3.更多语言和在线调试
其他编程语言的接入代码可以在 API调试页面 进行代码生成，同时支持在线进行接口调试。

---

# 接口汇总

| 接口 | 地址 | 用途 |
| --- | --- | --- |
| 记忆对话 | POST /v1/chat/memory/completions | 文字对话（带记忆） |
| 工作流运行 | POST /v1/workflow/run | 运行工作流 |
| 语音识别 | POST /v1/audio/transcriptions | 语音转文字 |
| 语音合成 | POST /v1/audio/speech | 文字转语音 |


图像识别
支持用户输入图片并根据图片进行问答，使用前提：

对于 应用接入：应用中需启用 "图像识别" 插件
对于 工作流接入：工作流中需使用 "图像识别" 插件
curl --request POST \
  --url https://api.link-ai.tech/v1/chat/memory/completions \
  --header 'Authorization: Bearer YOUR_API_KEY' \
  --header 'Content-Type: application/json' \
  --data '{
    "app_code": "",
    "question": "这张图中是什么?",
    "session_id": "123e4567-e89b-12d3-a456-426614174000",
    "image_url": "https://cdn.link-ai.tech/doc/vision-model-config.jpg"
}'

注意：

在 YOUR_API_KEY 处填入你创建的 API Key，将 app_code 中的值替换为你创建的应用或工作流的code。
图片url需填写公开网络可访问的图片地址




# 工作流运行接口
版本要求： 标准版 及以上

## 介绍
该接口为用于 工作流运行 的高级接口，相较于 通用对话接口 中的工作流运行有以下额外特性：

支持传入开始节点中的自定义变量
支持图片类型和文件类型参数的输入
支持工作流节点中开启的上下文记忆
在线接口调试：API接口调试

## 接口定义
接口地址
POST https://api.link-ai.tech/v1/workflow/run

## 请求头
参数	取值	说明
Authorization	Bearer YOUR_API_KEY	参考 接口鉴权说明 创建 API Key 并填入
Content-Type	application/json	表明使用JSON格式请求
请求体
参数	类型	是否必传	说明
app_code	string	是	工作流的唯一 code
args	dict	否	工作流输入变量，包括系统变量和自定义变量，在工作流的开始节点中定义
session_id	string	否	会话ID，每个session_id有独立的上下文记忆存储，大模型、应用或意图识别节点开启记忆后即可使用。若该字段不填则系统会自动分配一个唯一标识，在响应中返回，下次请求时可传入该值
工作流变量说明：

1.开始节点中支持的默认变量有 文字输入、图片输入、文件输入，参数名如下：

input_text    # 文字输入
input_image   # 字符串类型，图片url需公开网络可访问，若开启"多图片上传"，则传入URL列表
input_file    # 字符串类型，图片url需公开网络可访问，若开启"多文件上传"，则传入URL列表

可在工作流的开始节点开启系统变量。开启后在 args 参数中传入变量值即可，未启用系统变量时可不传该参数：



2.除系统变量外，还支持创建 自定义变量：

新增自定义变量时可设置变量的英文名，api调用时通过args参数传入变量值即可：



请求示例:

{
    "app_code": "G7z6vKwp",
    "args": {
        "input_text": "",          # 系统默认变量
        "height": "1.8",           # 自定义变量
        "weight": "75"             # 自定义变量
    }
}

注意：

app_code：你创建的工作流的code
args：填入流程中需要的系统变量或自定义变量的值
示例工作流地址：https://link-ai.tech/app/T8kjAqQw7o

响应结果
output_text 字段中为工作流的文本输出：

{
    "success": true,
    "code": 200,
    "message": "success",
    "data": {
        "output_text": "您好！您的BMI值是23.15，这个结果处于正常范围（18.5-23.9之间）。恭喜您，保持在这个范围内是健康的标志！\n\n为了进一步维护您的健康，建议您继续保持均衡的饮食和适量的运动。可以考虑增加一些富含纤维的食物，比如水果、蔬菜和全谷物，同时减少高糖和高脂肪食物的摄入。此外，每周至少进行150分钟的中等强度锻炼，比如快走、游泳或骑自行车，都是非常有益的。"
    }
}


错误说明
当接口异常时会返回以下结构：

{
    "success": false,
    "code": 408,
    "message": "当前版本无该API访问权限",
    "data": null
}

根据 HTTP状态码 (status code) 和错误信息 判断错误类型：

HTTP状态码	描述
400	请求格式错误
401	接口鉴权失败，请检查 API Key 是否填写正确
402	应用不存在，请检查 app_code 参数是否正确
403	无访问权限，对于未公开应用，只有创建者账号才能调用
406	账号积分额度不足
408	无API访问权限，该API支持标准版及以上版本调用
409	内容审核不通过，问题、回答、检索的知识库中可能存在敏感词
503	接口调用异常，联系客服处理
## 示例代码
### 1.CURL请求
curl -X POST "https://api.link-ai.tech/v1/workflow/run" \
    -H "Content-Type: application/json" \
    -H "authorization: Bearer YOUR_API_KEY" \
    -d '{
        "app_code": "T8kjAqQw7o",
        "args": {
            "height": "1.8",
            "weight": "75"
        }
    }'

注意：在 YOUR_API_KEY 处填入你创建的 API Key，在 app_code 中填入你创建的工作流code。

示例工作流地址：https://link-ai.tech/app/T8kjAqQw7o

### 2.Python代码请求
import requests

url = "https://api.link-ai.tech/v1/workflow/run"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer YOUR_API_KEY"
}
body = {
    "app_code": "T8kjAqQw7o",
    "args": {
        "height": "1.8",
        "weight": "75"
    }
}
res = requests.post(url, json=body, headers=headers)
if res.status_code == 200:
    res_json = res.json()
    reply_text = res_json.get("data").get("output_text")
    print(f"result={reply_text}")
else:
    error = res.json()
    print(f"请求异常, 错误码={error.get('code')}, 错误信息={error.get('message')}")

注意：在 YOUR_API_KEY 处填入你创建的 API Key，在 app_code 中填入你创建的工作流code，在 args 中填入系统变量或你设置的自定义变量。

###  3.更多语言和在线调试
其他编程语言的接入代码可以在 API调试页面 进行代码生成，同时支持在线进行接口调试。

#语音识别接口
版本要求： 标准版 及以上

##介绍
语音识别接口，可将语音文件转为文本内容，支持多种语言。

在线接口调试：API接口调试

##接口定义
接口地址
POST https://api.link-ai.tech/v1/audio/transcriptions

###请求头
参数	取值	说明
Authorization	Bearer YOUR_API_KEY	参考 接口鉴权说明 创建 API Key 并填入
Content-Type	multipart/form-data	表明使用form表单格式上传语音文件
请求体
参数	类型	是否必传	说明
file	file	是	语音文件，支持 mp3, mp4, mpeg, mpga, m4a, ogg, wav, or webm, flac, amr 类型
响应结果
text 字段中为识别出的文本内容：

{
    "text": "你好，我需要一些帮助"
}

###错误说明
当接口异常时会返回以下结构：

{
    "success": false,
    "code": 408,
    "message": "当前版本无该API访问权限",
    "data": null
}

###根据 HTTP状态码 (status code) 和错误信息 判断错误类型：

HTTP状态码	描述
400	请求格式错误
401	接口鉴权失败，请检查 API Key 是否填写正确
402	应用不存在，请检查 app_code 参数是否正确
403	无访问权限，对于未公开应用，只有创建者账号才能调用
406	账号积分额度不足
408	无API访问权限，该API支持标准版及以上版本调用
409	内容审核不通过，问题、回答、检索的知识库中可能存在敏感词
503	接口调用异常，联系客服处理
##示例代码
###1.CURL请求
curl https://api.link-ai.tech/v1/audio/transcriptions \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: multipart/form-data" \
  -F file="@/path/to/file/audio.mp3"

注意：在 YOUR_API_KEY 处填入你创建的 API Key，在file参数中填入音频文件的本地路径。

###2.Python代码请求
import requests

file_path = '/path/to/file/audio.mp3'      # 替换为你要上传的音频文件路径

url = 'https://api.link-ai.tech/v1/audio/transcriptions'
headers = {
    'Authorization': f'Bearer YOUR_API_KEY'
}
files = {
    'file': open(file_path, 'rb')
}
res = requests.post(url, headers=headers, files=files)
if res.status_code == 200:
    res_json = res.json()
    reply_text = res_json.get("text")
    print(f"text={reply_text}")
else:
    error = res.json()
    print(f"请求异常, 错误码={error.get('code')}, 错误信息={error.get('message')}")

注意：在 YOUR_API_KEY 处填入你创建的 API Key，在 file_path 变量中填入音频文件的本地路径。

###3.更多语言和在线调试
其他编程语言的接入代码可以在 API调试页面 进行代码生成，同时支持在线进行接口调试。