
# AWS S3 Plugin
Maven plugin for downloading artifacts from S3 bucket.

## Goals
### Download
Downloads the specified file based on the MD5 sum of the local file. If the file doesn't exist it will be downloaded anyway.

Currently supports publicly available objects only.
#### Prerequisites
The uploaded S3 object MUST have the custom metadata tag `x-amz-meta-md5` containing the MD5 checksum of  the uploaded object.

#### Usage
```
<plugin>  
	<groupId>com.apigate.maven</groupId>  
	<artifactId>aws.s3.plugin</artifactId>  
	<version>1.0.0-SNAPSHOT</version>  
	<executions>
		<execution>
			 <goals>
				 <goal>download</goal>  
			 </goals>
			 <phase>compile</phase>  
		</execution>
	</executions>
	<configuration>
		<url>https://s3.amazonaws.com/test.txt</url>
		<targetDir>/home/user1/Documents/</targetDir>  
		<md5Header>x-amz-meta-test</md5Header>  <!--required only if md5 tag is not x-amz-meta-md5. See above.-->
	</configuration>
 </plugin>
 ```
 ### Configurations
 - **url** *(required)* - S3 object url
 - **targetDir** *(required)* - Target download directory for this object
 - **md5Header** - The default header expected is `x-amz-meta-md5`. However should have have another header, this configuration property should be specified. This can be omitted if using the default.
