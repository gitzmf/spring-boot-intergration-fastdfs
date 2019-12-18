package com.util.fastdfs;

import lombok.Data;

/**
 * @author zmf
 * @version 1.0
 * @ClassName FastDFSFile
 * @Description:
 * @date 2019/12/6 17:44
 */
@Data
public class FastDFSFile {
    private String name;

    private byte[] content;

    private String ext;

    private String md5;

    private String author;

    public FastDFSFile(String name, byte[] content, String ext) {
        this.name = name;
        this.content = content;
        this.ext = ext;
    }
}
