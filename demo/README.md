# demo-service：Spring Boot + Kubernetes 学习示例

这是一个最小 Spring Boot Java 服务，用来练习 Kubernetes 部署流程。你可以用它在一主二从 K8s 集群中学习：打包、构建镜像、部署 Deployment、暴露 Service、查看日志、扩缩容和滚动更新。

## 项目结构

```text
demo/
├── pom.xml
├── Dockerfile
├── k8s/
│   ├── deployment.yaml
│   └── service.yaml
└── src/main/
    ├── java/com/example/
    │   ├── DemoApplication.java
    │   └── HelloController.java
    └── resources/application.yml
```

## 接口

启动后默认监听 `8080` 端口：

- `GET /`：返回服务信息、hostname 和当前时间
- `GET /hello`：返回当前 Pod/容器 hostname
- `GET /health`：返回 `OK`，用于健康检查
- `GET /env`：返回 K8s downward API 注入的环境变量

## 1. 本地打包

```bash
cd demo
mvn clean package -DskipTests
```

成功后会生成：

```text
target/demo-1.0-SNAPSHOT.jar
```

## 2. 本地运行

```bash
java -jar target/demo-1.0-SNAPSHOT.jar
```

另开一个终端测试：

```bash
curl http://localhost:8080/hello
curl http://localhost:8080/health
curl http://localhost:8080/env
```

## 3. 构建 Docker 镜像

```bash
docker build -t demo-service:1.0.0 .
```

本地容器测试：

```bash
docker run --rm -p 8080:8080 demo-service:1.0.0
```

测试：

```bash
curl http://localhost:8080/hello
```

## 4. 让 K8s 集群能拉到镜像

K8s 的每个 worker 节点都需要能拿到 `demo-service:1.0.0` 镜像。常见方式有三种。

### 方式 A：推送到 Docker Hub

把 `yourname` 换成你的 Docker Hub 用户名：

```bash
docker tag demo-service:1.0.0 yourname/demo-service:1.0.0
docker push yourname/demo-service:1.0.0
```

然后修改 `k8s/deployment.yaml`：

```yaml
image: yourname/demo-service:1.0.0
```

### 方式 B：推送到 Harbor 私有仓库

示例：

```bash
docker tag demo-service:1.0.0 harbor.example.com/test/demo-service:1.0.0
docker push harbor.example.com/test/demo-service:1.0.0
```

然后修改 `k8s/deployment.yaml`：

```yaml
image: harbor.example.com/test/demo-service:1.0.0
```

### 方式 C：导入镜像到每个 worker 节点

如果只是学习，也可以把镜像导出再导入到每个 worker 节点。

```bash
docker save demo-service:1.0.0 -o demo-service-1.0.0.tar
```

复制到 worker 节点后，如果你的 K8s 使用 containerd：

```bash
ctr -n k8s.io images import demo-service-1.0.0.tar
```

当前 `deployment.yaml` 使用：

```yaml
imagePullPolicy: IfNotPresent
```

只要节点本地已经有 `demo-service:1.0.0`，就会直接使用本地镜像。

## 5. 部署到 Kubernetes

在 master 节点或有 `kubectl` 的机器上执行：

```bash
kubectl apply -f k8s/
```

查看资源：

```bash
kubectl get deployment
kubectl get pod -o wide
kubectl get svc demo-service
```

你应该能看到 2 个 Pod：

```text
NAME                            READY   STATUS    NODE
demo-service-xxxxxxxxxx-xxxxx   1/1     Running   worker1
demo-service-xxxxxxxxxx-yyyyy   1/1     Running   worker2
```

## 6. 访问服务

`Service` 使用了 `NodePort`，端口固定为 `30080`。

用任意节点 IP 访问：

```bash
curl http://<节点IP>:30080/hello
```

例如：

```bash
curl http://192.168.1.101:30080/hello
```

多访问几次，返回的 Pod 名可能不同，说明 Service 正在做负载均衡。

## 7. 常用排查命令

查看 Pod：

```bash
kubectl get pod -o wide
```

查看 Pod 详情：

```bash
kubectl describe pod <pod-name>
```

查看日志：

```bash
kubectl logs -l app=demo-service
```

进入容器：

```bash
kubectl exec -it deploy/demo-service -- sh
```

查看环境变量：

```bash
kubectl exec deploy/demo-service -- env | grep MY_POD
```

## 8. 扩缩容练习

扩容到 3 个副本：

```bash
kubectl scale deployment demo-service --replicas=3
kubectl get pod -o wide
```

缩容到 1 个副本：

```bash
kubectl scale deployment demo-service --replicas=1
kubectl get pod -o wide
```

## 9. 滚动更新练习

修改代码后重新构建新版本镜像：

```bash
docker build -t demo-service:1.0.1 .
```

如果使用镜像仓库，推送新镜像后执行：

```bash
kubectl set image deployment/demo-service demo-service=yourname/demo-service:1.0.1
kubectl rollout status deployment/demo-service
```

查看历史：

```bash
kubectl rollout history deployment/demo-service
```

回滚：

```bash
kubectl rollout undo deployment/demo-service
```

## 10. 清理资源

```bash
kubectl delete -f k8s/
```
