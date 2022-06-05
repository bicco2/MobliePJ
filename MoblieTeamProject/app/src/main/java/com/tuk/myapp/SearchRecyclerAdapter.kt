package com.tuk.myapp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tuk.myapp.databinding.ViewholderSearchResultItemBinding

//어뎁터 구현
class SearchRecyclerAdapter : RecyclerView.Adapter<SearchRecyclerAdapter.SearchResultViewHolder>() {

    private var searchResultList: List<SearchResultEntity> = listOf()
    var currentPage = 1
    var currentSearchString = ""

    private lateinit var searchResultClickListener: (SearchResultEntity) -> Unit

    inner class SearchResultViewHolder(
        private val binding: ViewholderSearchResultItemBinding,
        private val searchResultClickListener: (SearchResultEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(data: SearchResultEntity) = with(binding) {
            titleTextView.text = data.name  // 타이틀 //한국공학대
            subtitleTextView.text = data.fullAddress //서브타이틀 //경기도 시흥시 ~
        }

        fun bindViews(data: SearchResultEntity) {
            // 검색 엔터누르면??
            binding.root.setOnClickListener {
                searchResultClickListener(data)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ViewholderSearchResultItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchResultViewHolder(binding, searchResultClickListener)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bindData(searchResultList[position])
        holder.bindViews(searchResultList[position])
    }

    override fun getItemCount(): Int {
        return searchResultList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSearchResultList( //검색 결과 리스트에 담음
        searchResultList: List<SearchResultEntity>,
        searchResultClickListener: (SearchResultEntity) -> Unit
    ) {
        this.searchResultList = this.searchResultList + searchResultList // 무한 스크롤 기능  -> 기존 리스트에 더해줌
        this.searchResultClickListener = searchResultClickListener
        notifyDataSetChanged()
    }

    fun clearList(){ // 리스트 초기화
        searchResultList = listOf()
    }


}