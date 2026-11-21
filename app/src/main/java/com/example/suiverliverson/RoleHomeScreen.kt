package com.example.suiverliverson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.example.suiverliverson.ui.theme.Red
import com.example.suiverliverson.ui.theme.White

@Composable
fun RoleHomeScreen(account: Account, onLogout: () -> Unit) {
    when (account.role) {
        AccountRole.CLIENT -> ClientDashboard(account, onLogout)
        AccountRole.COMPANY -> CompanyDashboard(account, onLogout)
        AccountRole.DRIVER -> ClientDashboard(account, onLogout) // fallback if driver accesses generic role home
    }
}

@Composable
fun ClientDashboard(account: Account, onLogout: () -> Unit) {
    val displayName = "${account.firstName} ${account.lastName}".trim()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repository = remember { OrderRepository() }
    var orders by remember { mutableStateOf<List<DeliveryRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickup by remember { mutableStateOf("") }
    var delivery by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    DisposableEffect(uid) {
        var registration: ListenerRegistration? = null
        if (uid != null) {
            registration = repository.listenForOwner(uid, { result ->
                orders = result
                loading = false
                error = null
            }, { message ->
                loading = false
                error = message
            })
        } else {
            loading = false
            error = "Sign in again to access your client dashboard."
        }
        onDispose { registration?.remove() }
    }

    val activeCount = orders.count { it.status in listOf("requested", "accepted", "picked_up") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Client Portal · پورتال الزبون", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Welcome, $displayName", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "Log out", tint = Red) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Total Orders", orders.size.toString(), Modifier.weight(1f))
            SummaryCard("Active Deliveries", activeCount.toString(), Modifier.weight(1f))
            SummaryCard("Completed", orders.count { it.status == "delivered" }.toString(), Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Red)
                    Text("Request Trivex Delivery / طلب توصيل Trivex", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Enter delivery details. Live drivers will accept and fulfill your request instantly.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                OutlinedTextField(pickup, { pickup = it }, Modifier.fillMaxWidth(), label = { Text("Pickup Address / مكان الاستلام") }, singleLine = true)
                OutlinedTextField(delivery, { delivery = it }, Modifier.fillMaxWidth(), label = { Text("Delivery Address / مكان التوصيل") }, singleLine = true)
                OutlinedTextField(recipient, { recipient = it }, Modifier.fillMaxWidth(), label = { Text("Recipient Name / اسم المستلم") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Recipient Phone / هاتف المستلم") }, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Package Description / وصف الطرد") }, minLines = 2)
                Button(
                    onClick = {
                        submitting = true
                        notice = null
                        repository.create(account, pickup, delivery, recipient, phone, description) { result ->
                            submitting = false
                            if (result == null) {
                                pickup = ""; delivery = ""; recipient = ""; phone = ""; description = ""
                                notice = "Delivery requested successfully!"
                            } else notice = result
                        }
                    },
                    enabled = !submitting && pickup.isNotBlank() && delivery.isNotBlank() && recipient.isNotBlank() && phone.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp, color = White)
                    else Text("Submit Delivery Request", color = White)
                }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Your Delivery Orders", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator()
        else if (orders.isEmpty() && error == null) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp)) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Red)
                    Spacer(Modifier.height(8.dp))
                    Text("No orders placed yet", fontWeight = FontWeight.SemiBold)
                    Text("Use the form above to request your first delivery.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            orders.forEach { request ->
                DeliveryRequestCard(request)
            }
        }

        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out / تسجيل الخروج") }
    }
}

@Composable
fun CompanyDashboard(account: Account, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AccountStore(context) }
    var currentAccount by remember { mutableStateOf(account) }
    val displayName = currentAccount.companyName.ifBlank { "${currentAccount.firstName} ${currentAccount.lastName}" }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repository = remember { OrderRepository() }
    var orders by remember { mutableStateOf<List<DeliveryRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickup by remember { mutableStateOf("") }
    var delivery by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    var newEmployeeId by remember { mutableStateOf("") }
    var employeeNotice by remember { mutableStateOf<String?>(null) }

    DisposableEffect(uid) {
        var registration: ListenerRegistration? = null
        if (uid != null) {
            registration = repository.listenForOwner(uid, { result ->
                orders = result
                loading = false
                error = null
            }, { message ->
                loading = false
                error = message
            })
        } else {
            loading = false
            error = "Sign in again to access company management."
        }
        onDispose { registration?.remove() }
    }

    val activeCount = orders.count { it.status in listOf("requested", "accepted", "picked_up") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Company Portal · لوحة تحكم الشركة", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("Business Address: ${currentAccount.location}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "Log out", tint = Red) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Company Orders", orders.size.toString(), Modifier.weight(1f))
            SummaryCard("Active Shipments", activeCount.toString(), Modifier.weight(1f))
            SummaryCard("Affiliated Staff", currentAccount.employeeIds.size.toString(), Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Red)
                    Text("Manage Company Fleet & Staff / إدارة فريق وسائقين الشركة", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Add employee or driver identifiers to your corporate organization network.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                OutlinedTextField(
                    value = newEmployeeId,
                    onValueChange = { newEmployeeId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Driver / Employee ID or Email") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newEmployeeId.isNotBlank()) {
                            val updatedList = currentAccount.employeeIds + newEmployeeId.trim()
                            val updatedAccount = currentAccount.copy(employeeIds = updatedList)
                            store.updateAccount(updatedAccount)
                            currentAccount = updatedAccount
                            newEmployeeId = ""
                            employeeNotice = "Staff member added successfully."
                        }
                    },
                    enabled = newEmployeeId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text("Add Staff Member", color = White)
                }
                employeeNotice?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                if (currentAccount.employeeIds.isNotEmpty()) {
                    Text("Current Staff IDs: ${currentAccount.employeeIds.joinToString(", ")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Red)
                    Text("Create Corporate Delivery / إنشاء إرسالية للشركة", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Dispatch enterprise orders across your fleet and network.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                OutlinedTextField(pickup, { pickup = it }, Modifier.fillMaxWidth(), label = { Text("Pickup Location / موقع التحميل") }, singleLine = true)
                OutlinedTextField(delivery, { delivery = it }, Modifier.fillMaxWidth(), label = { Text("Delivery Destination / وجهة التسليم") }, singleLine = true)
                OutlinedTextField(recipient, { recipient = it }, Modifier.fillMaxWidth(), label = { Text("Client / Receiver / الزبون") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Contact Phone / الهاتف") }, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Cargo Details / تفاصيل الشحنة") }, minLines = 2)
                Button(
                    onClick = {
                        submitting = true
                        notice = null
                        repository.create(currentAccount, pickup, delivery, recipient, phone, description) { result ->
                            submitting = false
                            if (result == null) {
                                pickup = ""; delivery = ""; recipient = ""; phone = ""; description = ""
                                notice = "Corporate order dispatched successfully."
                            } else notice = result
                        }
                    },
                    enabled = !submitting && pickup.isNotBlank() && delivery.isNotBlank() && recipient.isNotBlank() && phone.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp, color = White)
                    else Text("Dispatch Corporate Order", color = White)
                }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Fleet & Employee Deliveries", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator()
        else if (orders.isEmpty() && error == null) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp)) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Red)
                    Spacer(Modifier.height(8.dp))
                    Text("No corporate shipments found", fontWeight = FontWeight.SemiBold)
                    Text("Dispatch orders above to track company fleet activity.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            orders.forEach { request ->
                DeliveryRequestCard(request)
            }
        }

        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out / تسجيل الخروج") }
    }
}

@Composable
fun ApprovalPendingScreen(account: Account, onLogout: () -> Unit) {
    val displayName = if (account.role == AccountRole.COMPANY && account.companyName.isNotBlank()) {
        account.companyName
    } else {
        "${account.firstName} ${account.lastName}".trim()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Account review", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Log out", tint = Red)
            }
        }
        Text("Hello, $displayName", fontSize = 18.sp)
        Text(
            "Your ${account.role.label.lowercase()} account is waiting for approval. " +
                "You can use delivery features after the account is approved.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "After approval, log out and sign in again to refresh your account status.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Log out")
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeliveryRequestCard(request: DeliveryRequest) {
    val statusLabel = when (request.status) {
        "requested" -> "Waiting for a driver"
        "accepted" -> "Driver assigned"
        "picked_up" -> "Picked up"
        "delivered" -> "Delivered"
        "cancelled" -> "Cancelled"
        else -> request.status.replaceFirstChar { it.uppercase() }
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Request #${request.id.takeLast(6).uppercase()}", fontWeight = FontWeight.Bold)
                Text(statusLabel, color = if (request.status == "delivered") MaterialTheme.colorScheme.primary else Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("From: ${request.pickup}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("To: ${request.delivery}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("Recipient: ${request.recipientName} · ${request.recipientPhone}", fontSize = 12.sp)
            if (request.packageDescription.isNotBlank()) Text(request.packageDescription, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (request.driverUid.isNullOrBlank()) "No driver assigned yet" else "A driver has accepted this request", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
