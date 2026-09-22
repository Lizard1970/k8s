# k8s-page 静态页镜像 —— 以 nginx 承载水墨集页面
FROM nginx:1.27-alpine

COPY index.html /usr/share/nginx/html/index.html
COPY assets /usr/share/nginx/html/assets

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s CMD wget -q -O - http://127.0.0.1/ || exit 1
