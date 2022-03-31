FROM httpd:2.4.53
RUN mkdir -p /usr/local/apache2/exlibris_project
COPY ./feeds.json /usr/local/apache2/exlibris_project/feeds.json
COPY ./my-httpd.conf /usr/local/apache2/conf/httpd.conf
COPY ./httpd-vhosts.conf /usr/local/apache2/conf/extra/httpd-vhosts.conf
