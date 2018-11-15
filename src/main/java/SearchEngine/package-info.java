/**
 * 网络爬虫，对搜狐、百度、网易等新闻网站爬取新闻标题,
 * 然后把这些新闻标题和它的链接地址上传到hdfs多个文件上，
 * 一个文件对应一个标题和链接地址，
 * 然后通过分词技术对每个文件中的标题进行分词，分词后建立倒排索引以此来实现搜索引擎的功能。
 */
package SearchEngine;