package com.provider.service;

import com.client.dto.BookDTO;
import com.client.service.IBookService;

/**
 * @author litianxiang
 * @date 2020/3/6 15:25
 */
public class BookServiceImpl implements IBookService {
	public BookDTO getBookInfo(int id) {
		if (id == 1) {
			BookDTO bookDTO = new BookDTO();
			bookDTO.setId(1);
			bookDTO.setName("仙逆");
			bookDTO.setDesc("顺为凡, 逆为仙, 只在心中一念间.");
			bookDTO.setAuthor("耳根");
			return bookDTO;
		} else {
			return new BookDTO();
		}
	}
}
