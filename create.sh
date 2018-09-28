kafka-topics --zookeeper localhost:2181 --create --partitions 1 --replication-factor 1 --topic blue_backup
kafka-topics --zookeeper localhost:2181 --create --partitions 1 --replication-factor 1 --topic red_backup
kafka-topics --zookeeper localhost:2181 --create --partitions 1 --replication-factor 1 --topic joined_bucket_foo 
kafka-topics --zookeeper localhost:2181 --create --partitions 1 --replication-factor 1 --topic joined_bucket
kafka-topics --zookeeper localhost:2181 --create --partitions 1 --replication-factor 1 --topic red.nearest.airport
kafka-topics --zookeeper localhost:2181 --create --partitions 1 --replication-factor 1 --topic red.nearest.airport.count
kafka-topics --zookeeper localhost:2181 --list
