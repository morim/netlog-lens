package com.morim.netloglens.core

/** Cisco IOS / IOS-XE（Catalyst、ISRなど） */
object CiscoIosProfile : DeviceProfile {
    override val id = "cisco_ios"
    override val displayName = "Cisco IOS / IOS-XE"
    override val expertise = "Cisco IOS / IOS-XE（Catalyst・ISR・ASR）"

    override val promptHints = listOf(
        "syslogは「%FACILITY-SEVERITY-MNEMONIC: メッセージ」形式です。SEVERITYの数字（0=emergencies〜7=debugging）も判定に使ってください。",
        "インターフェース名は Gi=GigabitEthernet、Te=TenGigabitEthernet、Fa=FastEthernet、Po=Port-channel、Vl=Vlan の略です。",
        "show interfaces のエラーカウンタ（CRC、input errors、output drops など）が出ていれば、物理層・デュプレックス・輻輳のどれが疑わしいかも述べてください。",
    )

    // 伏せ字は共通ルール（CommonMaskRules）で足りるので、Cisco固有の追加ルールは無し
}
