jmeter -n -t jmeter/getProduct-1-500-concurrency.jmx \
  -Jprotocol=http -Jhost=127.0.0.1 -Jport=8080 \
  -JrangeThreads=500 -JrangeLoops=1 -Jramp=1 \
  -JreadThreads=400 -JreadThreads2=400 -JmissThreads=200 -JwriteThreads=5 -Jramp=20 \
  -l /tmp/getProduct.jtl -e -o /tmp/getProduct-report


