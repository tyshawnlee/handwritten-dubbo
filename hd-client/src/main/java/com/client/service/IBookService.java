package com.client.service;

import com.client.dto.BookDTO;

/**
 * @author litianxiang
 * @date 2020/3/6 15:15
 */
public interface IBookService {
	BookDTO getBookInfo(int id);
}
