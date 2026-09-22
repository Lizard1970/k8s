#!/usr/bin/env bash
# k8s-page 一键部署脚本（在 master 节点或有 docker + kubectl 的机器上执行）
# 用法：把整个 k8s-page 目录拷到该机器，然后 bash deploy.sh
set -e

REGISTRY=192.168.139.30:5000
IMAGE=$REGISTRY/k8s-page:1.0.0
NODEPORT=30081

echo "==> 1/4 构建镜像"
docker build -t $IMAGE .

echo "==> 2/4 推送镜像到私有仓库"
docker push $IMAGE

echo "==> 3/4 部署到集群"
kubectl apply -f k8s/

echo "==> 4/4 等待就绪"
kubectl rollout status deployment/k8s-page --timeout=120s
kubectl get pod -o wide
kubectl get svc k8s-page

echo ""
echo "部署完成。访问方式："
for ip in $(kubectl get nodes -o jsonpath='{.items[*].status.addresses[?(@.type=="InternalIP")].address}'); do
  echo "  http://$ip:$NODEPORT"
done
