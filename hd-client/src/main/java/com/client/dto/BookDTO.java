package com.client.dto;

import java.io.Serializable;

/**
 * @author litianxiang
 * @date 2020/3/6 15:14
 */
public class BookDTO implements Serializable{

	private static final long serialVersionUID = 1934175717377394706L;

	private int id;
	private String name;
	private String desc;
	private String author;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	@Override
	public String toString() {
		return "BookDTO{" +
				"id=" + id +
				", name='" + name + '\'' +
				", desc='" + desc + '\'' +
				", author='" + author + '\'' +
				'}';
	}
}
