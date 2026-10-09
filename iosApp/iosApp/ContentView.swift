import SwiftUI

struct ExpenseItem: Identifiable, Codable {
    var id: String = UUID().uuidString
    var amount: Double
    var category: String
    var item: String
    var note: String
    var date: Date
}

struct ContentView: View {
    @State private var expenses: [ExpenseItem] = []
    @State private var selectedTab: Int = 0
    @State private var showAddExpense = false
    @State private var showAiAssistant = false
    @State private var currencySymbol = "₹"

    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 0.08, green: 0.08, blue: 0.12)
                    .ignoresSafeArea()

                VStack {
                    // Header
                    HStack {
                        Text("Where'd your money go?")
                            .font(.headline)
                            .foregroundColor(.white)

                        Spacer()

                        Button(action: { showAiAssistant.toggle() }) {
                            HStack {
                                Text("✦ AI")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(.white)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                LinearGradient(colors: [.blue, .purple, .pink], startPoint: .leading, endPoint: .trailing)
                            )
                            .cornerRadius(20)
                        }
                    }
                    .padding(.horizontal)

                    // Main Spend Card
                    VStack(alignment: .leading, spacing: 8) {
                        Text("You've spent")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.8))

                        Text("\(currencySymbol)\(totalSpend(), specifier: "%.0f")")
                            .font(.system(size: 38, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(
                        LinearGradient(colors: [Color(red: 0.48, green: 0.22, blue: 0.92), Color(red: 0.95, green: 0.24, blue: 0.36)], startPoint: .topLeading, endPoint: .bottomTrailing)
                    )
                    .cornerRadius(22)
                    .padding(.horizontal)

                    Spacer()
                }
            }
            .navigationBarHidden(true)
        }
    }

    private func totalSpend() -> Double {
        expenses.reduce(0) { $0 + $1.amount }
    }
}

#Preview {
    ContentView()
}
