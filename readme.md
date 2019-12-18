#FastDFS+Nginx环境搭建
##FastDFS安装
|软件包|版本号|
|---|:---:|
|FastDFS|v5.05|
|libfastcommon|v1.0.7|
###下载libfastcommon  
``wget https://github.com/happyfish100/libfastcommon/archive/V1.0.7.tar.gz
``  
2.解压  
``tar -zxvf V1.0.7.tar.gz
``  
3.编译、安装  
``cd V1.0.7/   
``  
``./make.sh      
``  
``./make.sh install
``  
4.创建软连接
``` 
ln -s /usr/lib64/libfastcommon.so /usr/local/lib/libfastcommon.so
ln -s /usr/lib64/libfastcommon.so /usr/lib/libfastcommon.so
ln -s /usr/lib64/libfdfsclient.so /usr/local/lib/libfdfsclient.so
ln -s /usr/lib64/libfdfsclient.so /usr/lib/libfdfsclient.so 
```
###安装FastDFS
1.下载FastDFS
```
wget https://github.com/happyfish100/fastdfs/archive/V5.05.tar.gz
```  
2.解压
```
tar -xvf V5.05.tar.gz
cd fastdfs-5.05/
```  
3.编译安装
```
./make.sh
./make.sh install
```  
####配置Tracker服务
上述安装成功后，在/etc/目录下会有一个fdfs的目录，进入它。会看到三个.sample后缀的文件，这是作者给我们的示例文件，我们需要把其中的tracker.conf.sample文件改为tracker.conf配置文件并修改它：
```
cp tracker.conf.sample tracker.conf
nano tracker.conf
```  
编辑tracker.conf
```
# 配置文件是否不生效，false 为生效
disabled=false

# 提供服务的端口
port=22122

# Tracker 数据和日志目录地址
base_path=/usr/lcoal/zmf/fastdfsData

# HTTP 服务端口
http.server_port=80
```
创建tracker基础数据目录，即base_path对应的目录
```
mkdir /usr/local/zmf/fastdfaData
```  
使用ln -s建立软连接
```
ln -s /usr/bin/fdfs_trackerd /usr/local/bin
ln -s /usr/bin/stop.sh /usr/local/bin
ln -s /usr/bin/restart.sh /usr/local/bin
```
启动服务
```
service fdfs_trackerd start
```
查看监听  
```
netstat -unltp|grep fdfs
```
如果看到22122端口正常被监听后，这时候说明Tracker服务启动成功啦！  
tracker server 目录及文件结构
Tracker服务启动成功后，会在base_path下创建data、logs两个目录。目录结构如下：
```
${base_path}
  |__data
  |   |__storage_groups.dat：存储分组信息
  |   |__storage_servers.dat：存储服务器列表
  |__logs
  |   |__trackerd.log： tracker server 日志文件 
```
####配置Storage服务
进入 /etc/fdfs 目录，复制 FastDFS 存储器样例配置文件 storage.conf.sample，并重命名为 storage.conf
```
cd /etc/fdfs
cp storage.conf.sample storage.conf
nano storage.conf
```  
编辑storage.conf
```
# 配置文件是否不生效，false 为生效
disabled=false

# 指定此 storage server 所在 组(卷)
group_name=group1

# storage server 服务端口
port=23000

# 心跳间隔时间，单位为秒 (这里是指主动向 tracker server 发送心跳)
heart_beat_interval=30

# Storage 数据和日志目录地址(根目录必须存在，子目录会自动生成)
base_path=/usr/local/zmf/fastdfaData/storage

# 存放文件时 storage server 支持多个路径。这里配置存放文件的基路径数目，通常只配一个目录。
store_path_count=1

# 逐一配置 store_path_count 个路径，索引号基于 0。
# 如果不配置 store_path0，那它就和 base_path 对应的路径一样。
store_path0=/usr/local/zmf/fastdfaData/storage

# FastDFS 存储文件时，采用了两级目录。这里配置存放文件的目录个数。 
# 如果本参数只为 N（如： 256），那么 storage server 在初次运行时，会在 store_path 下自动创建 N * N 个存放文件的子目录。
subdir_count_per_path=256

# tracker_server 的列表 ，会主动连接 tracker_server
# 有多个 tracker server 时，每个 tracker server 写一行
tracker_server=192.168.199.137:22122

# 允许系统同步的时间段 (默认是全天) 。一般用于避免高峰同步产生一些问题而设定。
sync_start_time=00:00
sync_end_time=23:59
```
创建storage基础数据目录，即base_path对应的目录
```
cd  /usr/local/zmf/fastdfaData
mkdir storage
``` 
使用ln -s建立软连接
```
ln -s /usr/bin/fdfs_storaged /usr/local/bin
```
启动服务
```
service fdfs_storaged start
```
查看监听  
```
netstat -unltp|grep fdfs
```
启动Storage前确保Tracker是启动的。初次启动成功，会在 /home/data/fastdfs/storage 目录下创建 data、 logs 两个目录。如果看到23000端口正常被监听后，这时候说明Storage服务启动成功啦！  
查看Storage和Tracker是否在通信
```
/usr/bin/fdfs_monitor /etc/fdfs/storage.conf
```
##FastDFS-Nginx-model与nginx(Openresty)安装
|软件包|版本号|
|---|:---:|
|openresty|v1.13.6.1|
|fastdfs-nginx-module|v1.1.6|
FastDFS通过Tracker服务器，将文件放在Storage服务器存储，但是同组存储服务器之间需要进行文件复制，有同步延迟的问题。
假设Tracker服务器将文件上传到了192.168.1.190，上传成功后文件ID已经返回给客户端。此时 FastDFS 存储集群机制会将这个
文件同步到同组存192.168.1.190，在文件还没有复制完成的情况下，客户端如果用这个文件ID在192.168.1.190上取文件,就会出现文件无法访问的错误。而fastdfs-nginx-module可以重定向文件链接到源服务器取文件，避免客户端由于复制延迟导致的文件无法访问错误。
下载安装Nginx和fastdfs-nginx-module：
推荐您使用yum安装以下的开发库:
```
yum install readline-devel pcre-devel openssl-devel -y
```
下载最新版本并解压
```
wget https://openresty.org/download/openresty-1.13.6.1.tar.gz

tar -xvf openresty-1.13.6.1.tar.gz

wget https://github.com/happyfish100/fastdfs-nginx-module/archive/master.zip

unzip master.zip
```
配置nginx安装，加入fastdfs-nginx-module模块：
```
cd  openresty-1.13.6.1
./configure --add-module=/usr/local/zmf/fastdfs-nginx-module-master/src/
```
编译、安装：
```
make && make install
```
查看nginx版本
```
/usr/local/openresty/nginx/sbin/nginx -V
```
如果出现fastdfs-nginx-modulexxx,则表示安装成功
复制fastdfs-nginx-module源码中的配置文件到/etc/fdfs目录，并修改：
```
cp /fastdfs-nginx-module/src/mod_fastdfs.conf /etc/fdfs/
```
编辑mod_fastdfs.conf
```
 连接超时时间
connect_timeout=10

# Tracker Server
tracker_server=192.168.1.190:22122

# StorageServer 默认端口
storage_server_port=23000

# 如果文件ID的uri中包含/group**，则要设置为true
url_have_group_name = true

# Storage 配置的store_path0路径，必须和storage.conf中的一致
store_path0=/usr/local/zmf/fastdfaData
```
复制FastDFS的部分配置文件到/etc/fdfs目录：
```
cp /fastdfs-5.05/src/http.conf /etc/fdfs/
cp /fastdfs-5.05/src/mime.types /etc/fdfs/
```
配置nginx，修改nginx.conf：
```
location ~/group([0-9])/M00 {
    ngx_fastdfs_module;
}
```
启动Nginx：
```
./nginx
ngx_http_fastdfs_set pid=9236
```
测试上传
```
/usr/bin/fdfs_upload_file /etc/fdfs/client.conf /usr/lcoal/zmf/iamge/hello.txt
```
##FastDFS-Nginx-model与nginx(普通)安装
|软件包|版本号|
|---|:---:|
|openresty|v1.10.3|
|fastdfs-nginx-module|修复版|
推荐您使用yum安装以下的开发库:
```
yum install readline-devel pcre-devel openssl-devel -y
```
下载最新版本并解压
```
wget https://openresty.org/download/openresty-1.13.6.1.tar.gz

tar -xvf openresty-1.13.6.1.tar.gz

wget https://github.com/happyfish100/fastdfs-nginx-module/archive/master.zip

unzip master.zip
```
配置nginx安装，加入fastdfs-nginx-module模块：
```
cd  /usr/local/zmf/nginx/sbin/
#查看现有模块
./nginx -V
显示结果:./configure --prefix=/usr/local/zmf/nginx --conf--with-http_ssl_module
#在现有的配置上增加fastdfs-nginx-module
/configure --prefix=/usr/local/zmf/nginx --conf-path=/usr/local/zmf/nginx/conf/nginx.conf  --conf--with-http_ssl_module --add-module=/usr/local/zmf/fastdfs-nginx-module-master/src/
注意：添加模块的时候，一定要先查看现有的配置信息，在现有的配置信息上增加模块，否则后面编译后，会只有增加的模块，之前安装的所有配置信息
不再存在，编译后会覆盖原来的编译信息
```
编译、安装：
```
make && make install
```
查看nginx版本
```
/usr/local/zmf/nginx/sbin/nginx -V
```
如果出现fastdfs-nginx-modulexxx,则表示安装成功
复制fastdfs-nginx-module源码中的配置文件到/etc/fdfs目录，并修改：
```
cp /fastdfs-nginx-module/src/mod_fastdfs.conf /etc/fdfs/
```
编辑mod_fastdfs.conf
```
 连接超时时间
connect_timeout=10

# Tracker Server
tracker_server=192.168.1.190:22122

# StorageServer 默认端口
storage_server_port=23000

# 如果文件ID的uri中包含/group**，则要设置为true
url_have_group_name = true

# Storage 配置的store_path0路径，必须和storage.conf中的一致
store_path0=/usr/local/zmf/fastdfaData
```
复制FastDFS的部分配置文件到/etc/fdfs目录：
```
cp /fastdfs-5.05/src/http.conf /etc/fdfs/
cp /fastdfs-5.05/src/mime.types /etc/fdfs/
```
配置nginx，修改nginx.conf：
```
location ~/group([0-9])/M00 {
    ngx_fastdfs_module;
}
```
启动Nginx：
```
./nginx
ngx_http_fastdfs_set pid=9236
```
测试上传
```
/usr/bin/fdfs_upload_file /etc/fdfs/client.conf /usr/lcoal/zmf/iamge/hello.txt
```
##安装过程出现问题以及解决方案
1. 问题1：make过程出现cp: `conf/koi-win' and `/usr/local/nginx/conf/koi-win' are the same file
```
此问题并不影响，原因是配置文件--conf-path=/usr/local/zmf/nginx/conf/nginx.conf,配置文件制定的时解压包目录下的
配置文件路径，nginx并不允许，所以会报错，并不影响，只要./nginx -V限制正常即可。如果想解决错误，不指定--conf-path即可
或者指定--conf-path=/usr/local/nginx/conf/nginx.conf 
注意，如果不指定，会默认在/usr/local/生成个nginx文件夹，此时模块是安装在此处，因此配置文件也是此处的有效
```
2.问题2：make过程出现fatal error: common_define.h: No such file or directory 的解决办法
```
这是fastdfs-nginx模块的bug,需要修改fastdfs-nginx-module/src/config配置文件
缺省的：
    ngx_module_incs="/usr/include"
    CORE_INCS="$CORE_INCS /usr/include"
修改后的：
    ngx_module_incs="/usr/include/fastdfs /usr/include/fastcommon/"
    CORE_INCS="$CORE_INCS /usr/include/fastdfs /usr/include/fastcommon/"
现在再去编译安装Nginx就可以了。
```
3.问题3：问题2配置后，make后依然出现问题..../usr/local/FastDFS/fastdfs-nginx-module-master/src//common.c:1245:61: error: ‘FDFSHTTPParams’ has no member named ‘support_multi_range’....等错误
```
# 这里为啥这么长一串呢，因为最新版的master与当前nginx有些版本问题。所以使用别人修复包的fastdfs-nginx-model
wget https://github.com/happyfish100/fastdfs-nginx-module/archive/5e5f3566bbfa57418b5506aaefbe107a42c9fcb1.zip
```
4.问题4：nginx整合fastdfs后，启动没有worker进程原因
```
fastdfs/src/下面的http.conf和mime.types拷贝到/etc/fdfs下面
fastdfs-nginx-module-test/src/的mod_fastdfs.conf拷贝至/etc/fdfs/下
```  