package com.example.firebase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.lifecycle.MutableLiveData
import com.example.firebase.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    //firebase
    private val firestore = FirebaseFirestore.getInstance()

    private val budgetCollectionRef = firestore.collection("budgets")
    //nampung id
    private var updateId = ""
    //nampung list dari data bbudget
    private val budgetListLiveData: MutableLiveData<List<Budget>> by lazy {
        MutableLiveData<List<Budget>>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding){
            btnAdd.setOnClickListener {
                val nominal = edtNominal.text.toString()
                val desc = edtDesc.text.toString()
                val date = edtDate.text.toString()
                val newBudget = Budget(
                    nominal = nominal,
                    desc = desc,
                    date = date
                )
                addData(newBudget)
            }
            btnUpdate.setOnClickListener {
                val nominal = edtNominal.text.toString()
                val desc = edtDesc.text.toString()
                val date = edtDate.text.toString()
                val budgetUpdate = Budget(
                    nominal = nominal,
                    desc = desc,
                    date = date
                )
                updateData(budgetUpdate)
                updateId = ""
                resetForm()
            }

            listView.setOnItemClickListener { viewAdapter, view, position, id ->
                val item = viewAdapter.adapter.getItem(position) as Budget
                updateId = item.id
                edtNominal.setText(item.nominal)
                edtDesc.setText(item.desc)
                edtDate.setText(item.date)
            }

            listView.setOnItemLongClickListener { viewAdapter, view, position, id ->
                val item = viewAdapter.adapter.getItem(position) as Budget
                deleteData(item)
                true
            }
        }
        observeBudgets()
        getAllBudgets()
    }

    private fun getAllBudgets(){
        observeBudgetChanges()
    }

    private fun observeBudgetChanges(){
        budgetCollectionRef.addSnapshotListener{ snapshot, error ->
            if (error != null) {
                Log.d("MainActivity", "Error listening for budget change: ", error)
                return@addSnapshotListener
            }
            val budgets = snapshot?.toObjects(Budget::class.java)
            if (budgets!= null) {
                budgetListLiveData.postValue(budgets)
            }
        }
    }
    //update list data dari data budget yg diperbarui
    private fun observeBudgets() {
        budgetListLiveData.observe(this) { budgets ->
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                budgets.toMutableList()
            )
            binding.listView.adapter = adapter
        }
    }
    // dihandle untuk kondisi sukses, akan mereturn docRef
    private fun addData(budget: Budget) {
        budgetCollectionRef.add(budget)
            .addOnSuccessListener { docRef ->
                val createBudgetId = docRef.id
                //id nya di update sesuai id yang berhasil
                budget.id = createBudgetId
                docRef.set(budget)
                    .addOnFailureListener{
                        Log.d("MainActivity", "Error update budget id", it)
                    }
                resetForm()
            }
            .addOnFailureListener{
                Log.d("MainActivity", "Error add budget", it)
            }
    }

    private fun resetForm(){
        with(binding){
            edtNominal.setText("")
            edtDesc.setText("")
            edtDate.setText("")
        }
    }

    private fun updateData(budget: Budget){
        budget.id = updateId
        budgetCollectionRef.document(updateId).set(budget)
            .addOnFailureListener{
                Log.d("MainActivity", "Error update data budget", it)
            }
    }

    private fun deleteData(budget: Budget){
        if (budget.id.isEmpty()) {
            Log.d("MainActivity", "Error delete data empty ID", return)
        }

        budgetCollectionRef.document(budget.id).delete()
            .addOnFailureListener{
                Log.d("MainActivity", "Error delete data budget")
            }
    }
}