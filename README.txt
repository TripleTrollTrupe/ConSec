O programa deve ser executado da seguinte maneira

Usando o Eclipse:
	Do lado do servidor
 		Dentro das Run Configurations da classe PhotoShareServer deve-se aceder a tab "Arguments" e colocar nos campos indicados
 		os seguintes argumentos:
 			-Program arguments: <porto> (neste projecto o servidor deve correr no porto 23456)
 			-VM arguments:  -Djava.security.manager 
 							-Djava.security.policy=server.policy
							 http://−Djavax.net.ssl.keyStore=/keytool/serverkeystore.jks
							 http://−Djavax.net.ssl.keyStorePassword=requiem

 			
 	Do lado do cliente
 		Dentro das Run Configurations da classe PhotoShareCliente deve-se aceder a tab "Arguments" e colocar nos campos indicados
 		os seguintes argumentos:
 			-Program arguments: -u <localUserId> -a <serverAddress> [-p <photos>| -l <userId> | -g <userId> | -c <comment> <userId>
 								 <photo> | -f <followUserIds> | - n]
 			-VM arguments: -Djava.security.manager -Djava.security.policy=client.policy
							http://−Djavax.net.ssl.trustStore=/keytool/clientkeystore.jks
 			
 	
De referir que os VM arguments sao passados de forma a que seja utilizada a sandbox desenhada pelo grupo



Se for pretendido executar o programa usando apenas o terminal deve se executar os seguintes comandos
Nota: partindo do suposto que nos encontramos na directoria das classes respectivas

	Do lado do servidor
	java -Djava.security.manager 
		 -Djava.security.policy=server.policy
		 竏奪javax.net.ssl.keyStore=.\keytool\serverkeystore.jks
		 竏奪javax.net.ssl.keyStorePassword=requiem
		PhotoShareServer <port>

	Do lado do cliente
	
	-Djava.security.manager -Djava.security.policy=client.policy
	竏奪javax.net.ssl.trustStore=.\keytool\clientkeystore.jks
		PhotoShareCliente -u <localUserId> -a <serverAddress> [-p <photos>| -l <userId> | -g <userId> | 
		-c <comment> <userId> <photo> | -f <followUserIds> | - n]

