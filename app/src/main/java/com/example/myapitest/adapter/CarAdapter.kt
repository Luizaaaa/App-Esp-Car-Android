package com.example.myapitest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapitest.R
import com.example.myapitest.model.Car
import com.example.myapitest.ui.loadUrl

class CarAdapter(
    private val cars: List<Car>,
    private val carClickListener:(Car)-> Unit
): RecyclerView.Adapter<CarAdapter.CarViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_layout, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
       val car = cars[position]
       holder.model.text = car.name
       holder.year.text = car.year
       holder.license.text = car.licence
       holder.imageView.loadUrl(car.imageUrl)
       holder.itemView.setOnClickListener {
           carClickListener(car)
       }
    }

    override fun getItemCount(): Int = cars.size

    class CarViewHolder(view: View): RecyclerView.ViewHolder(view){
       val imageView: ImageView = view.findViewById(R.id.image)
       val model: TextView = view.findViewById(R.id.model)
       val year: TextView = view.findViewById(R.id.year)
       val license: TextView = view.findViewById(R.id.license)
    }
}