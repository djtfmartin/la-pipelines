cd solr/conf

echo 'Zipping configset'
rm config.zip
zip config.zip *

echo 'Deleting existing collection'
curl -X GET "http://localhost:8985/solr/admin/collections?action=DELETE&name=avro-test"

echo 'Deleting existing collection'
curl -X GET "http://localhost:8985/solr/admin/collections?action=DELETE&name=biocache"

echo 'Deleting existing configset'
curl -X GET "http://localhost:8985/solr/admin/configs?action=DELETE&name=biocache&omitHeader=true"

echo 'Creating  configset'
curl -X POST --header "Content-Type:application/octet-stream" --data-binary @config.zip "http://localhost:8985/solr/admin/configs?action=UPLOAD&name=biocache"

echo 'Creating  collection'
curl -X GET "http://localhost:8985/solr/admin/collections?action=CREATE&name=biocache&numShards=32&maxShardsPerNode=4&replicationFactor=1&collection.configName=biocache"

cd ../..

rm solr/conf/config.zip

echo 'Done'



