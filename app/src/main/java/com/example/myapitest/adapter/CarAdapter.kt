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
    private val onItemClick: (Car) -> Unit,
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    class CarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView>(R.id.image)
        val nameTextView = view.findViewById<TextView>(R.id.name)
        val yearTextView = view.findViewById<TextView>(R.id.year)
        val licenceTextView = view.findViewById<TextView>(R.id.licence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_layout, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = cars[position]

        holder.nameTextView.text = car.name
        holder.yearTextView.text = holder.itemView.context.getString(R.string.car_label_year, car.year)
        holder.licenceTextView.text = holder.itemView.context.getString(R.string.car_label_licence, car.licence)
        holder.imageView.loadUrl(car.imageUrl)

        holder.itemView.setOnClickListener {
            onItemClick(car)
        }
    }

    override fun getItemCount(): Int = cars.size
}
