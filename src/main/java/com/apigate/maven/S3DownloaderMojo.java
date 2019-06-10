package com.apigate.maven;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@Mojo(name = "download")
public class S3DownloaderMojo extends AbstractMojo {

    @Parameter(property = "url")
    private String url;

    @Parameter(property = "targetDir")
    private String targetDir;

    @Parameter(property = "md5Header", defaultValue = "x-amz-meta-md5")
    private String md5Header;

    public void execute() throws MojoExecutionException, MojoFailureException {

        File file;
        validateProperties();


        try {

            GetRequest request = Unirest.head(url);
            HttpResponse<JsonNode> response = request.asJson();
            checkIfValidS3Object(response);

            String[] urlArray = url.split("/");

            String fname = urlArray[urlArray.length - 1];

            file = new File(targetDir + "/" + fname);
            if (file.exists()) {
                getLog().info("File exists: " + file.toString());
                if (response.getHeaders().containsKey(md5Header)) {
                    String md5Remote = response.getHeaders().get(md5Header).get(0);
                    String md5Local = DigestUtils.md5Hex(new FileInputStream(file));
                    if (!md5Local.equals(md5Remote)) {
                        getLog().info("MD5 mismatch. Local: " + md5Local + ". Remote: " + md5Remote);
                        downloadFile(file);
                    }

                } else {
                    throw new MojoExecutionException("Unable to find x-amz-meta-md5 header in response. Check S3 " +
                            "metadata" +
                            ". If using a different header name, ensure you set md5Header parameter in plugin " +
                            "configuration");
                }

            } else {
                downloadFile(file);
            }


        } catch (UnirestException e) {
            throw new MojoExecutionException("Error getting details from S3", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error opening file", e);
        }

    }

    /**
     * Validates all mandatory properties required for executing this plugin
     * If using default values, appropriate logs should be added
     *
     * @throws MojoExecutionException
     */
    private void validateProperties() throws MojoExecutionException {

        if (StringUtils.isEmpty(url)) {
            throw new MojoExecutionException("url not defined!");
        }

        if (StringUtils.isEmpty(targetDir)) {
            throw new MojoExecutionException("targetDir not defined!");
        }

    }

    /**
     * Checks to see if response headers contain the ETag header
     * Valid S3 objects are all assigned an Etag
     * @param response HttpResponse containing HEAD response
     * @throws MojoExecutionException
     */
    private void checkIfValidS3Object(HttpResponse<JsonNode> response) throws MojoExecutionException {

        if (!response.getHeaders().containsKey("ETag")) {
            throw new MojoExecutionException(url + ": Not a valid S3 object!");
        }

    }

    /**
     * Downloads S3 object to specified path
     * @param file Location to download file
     * @throws MojoExecutionException
     */
    private void downloadFile(File file) throws MojoExecutionException {

        try {
            getLog().info("Downloading file from: " + url + " to: " + targetDir);
            HttpResponse<InputStream> response = Unirest.get(url).asBinary();
            FileUtils.copyInputStreamToFile(response.getRawBody(), file);

            if (response.getHeaders().containsKey("Last-Modified")) {

                file.setLastModified(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(response.getHeaders().get("Last-Modified").get(0)).getTime());
            }
        } catch (UnirestException | IOException | ParseException e) {
            throw new MojoExecutionException("Error getting details from S3", e);
        }
    }

}
