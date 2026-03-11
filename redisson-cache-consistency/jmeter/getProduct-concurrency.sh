jmeter -n -t jmeter/getProduct-concurrency.jmx \
  -Jprotocol=http -Jhost=127.0.0.1 -Jport=8080 \
  -JproductId=1 -JproductId2=2 -JmissingProductId=999999 \
  -JreadThreads=400 -JreadThreads2=400 -JmissThreads=200 -JwriteThreads=5 -Jramp=20 \
  -l /tmp/getProduct.jtl -e -o /tmp/getProduct-report