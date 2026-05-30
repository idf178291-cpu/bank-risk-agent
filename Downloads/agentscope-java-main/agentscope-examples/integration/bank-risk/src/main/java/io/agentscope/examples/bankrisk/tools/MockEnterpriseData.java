package io.agentscope.examples.bankrisk.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MockEnterpriseData {

    public record EnterpriseInfo(
            String customerId,
            String name,
            String industry,
            String region,
            double carRatio,
            double nplRatio,
            double provisionCoverage,
            double liquidityRatio,
            double roa,
            List<String> riskTags,
            List<String> recentEvents,
            String riskLevel,
            String riskLevelLabel) {}

    private static final Map<String, EnterpriseInfo> DATA = new LinkedHashMap<>();

    static {
        put(
                new EnterpriseInfo(
                        "CUST-001",
                        "宁德时代新能源科技股份有限公司",
                        "新能源",
                        "华东",
                        18.5,
                        0.5,
                        320.0,
                        65.0,
                        8.2,
                        List.of("全球市场份额第一", "技术壁垒高", "现金流充裕", "客户结构优质"),
                        List.of("2025年与特斯拉续签长期供货协议", "2024年全年营收突破5000亿", "固态电池技术取得重大突破"),
                        "LOW",
                        "低风险"));

        put(
                new EnterpriseInfo(
                        "CUST-002",
                        "华为技术有限公司",
                        "通信设备",
                        "华南",
                        22.0,
                        0.3,
                        450.0,
                        72.0,
                        9.5,
                        List.of("研发投入领先", "全球化布局", "专利壁垒深厚", "财务极为稳健"),
                        List.of("2025年发布鸿蒙生态白皮书", "2024年企业业务增长超预期", "智能汽车解决方案加速落地"),
                        "LOW",
                        "低风险"));

        put(
                new EnterpriseInfo(
                        "CUST-003",
                        "比亚迪股份有限公司",
                        "新能源汽车",
                        "华南",
                        14.2,
                        1.1,
                        260.0,
                        55.0,
                        5.8,
                        List.of("新能源销量全球第一", "垂直整合能力强", "海外扩张加速", "负债率偏高"),
                        List.of("2025年Q1新能源车出口增长120%", "2024年全年销量突破400万辆", "在匈牙利建设欧洲首个乘用车工厂"),
                        "LOW",
                        "低风险"));

        put(
                new EnterpriseInfo(
                        "CUST-004",
                        "恒大集团有限公司",
                        "房地产",
                        "华南",
                        -15.0,
                        45.0,
                        20.0,
                        3.0,
                        -18.5,
                        List.of("资不抵债", "大规模债务违约", "项目大面积停工", "被多次起诉", "实控人被采取强制措施"),
                        List.of(
                                "2025年香港法院颁布清盘令", "2024年境内多项目被地方政府接管",
                                "2023年境外美元债全面违约", "2022年暂停所有在建项目施工"),
                        "CRITICAL",
                        "严重"));

        put(
                new EnterpriseInfo(
                        "CUST-005",
                        "海航集团有限公司",
                        "航空运输",
                        "华南",
                        2.1,
                        18.5,
                        65.0,
                        12.0,
                        -5.2,
                        List.of("破产重整完成", "资产大幅缩水", "负债率仍高", "业务持续亏损", "信用评级下调"),
                        List.of(
                                "2025年重整计划执行中部分债权人提出异议",
                                "2024年出售多家子公司以回笼资金",
                                "2023年完成破产重整程序",
                                "2021年正式进入破产重整"),
                        "HIGH",
                        "高风险"));

        put(
                new EnterpriseInfo(
                        "CUST-006",
                        "清华同方股份有限公司",
                        "信息技术",
                        "华北",
                        8.5,
                        4.2,
                        140.0,
                        28.0,
                        0.8,
                        List.of("盈利能力弱", "应收账款周转慢", "政府订单依赖度高", "技术竞争力下滑"),
                        List.of("2025年Q1应收账款逾期增加", "2024年部分政府项目回款延迟", "2023年剥离亏损的海外业务"),
                        "WATCH",
                        "关注"));

        put(
                new EnterpriseInfo(
                        "CUST-007",
                        "隆基绿能科技股份有限公司",
                        "新能源",
                        "西北",
                        16.8,
                        0.8,
                        350.0,
                        58.0,
                        7.5,
                        List.of("光伏硅片全球第一", "技术路线领先", "财务稳健", "海外市场拓展顺利"),
                        List.of("2025年BC电池技术大规模量产", "2024年组件出货量全球前三", "在马来西亚新建6GW电池产能"),
                        "LOW",
                        "低风险"));

        put(
                new EnterpriseInfo(
                        "CUST-008",
                        "阳光电源股份有限公司",
                        "新能源",
                        "华东",
                        15.2,
                        1.0,
                        280.0,
                        52.0,
                        6.8,
                        List.of("逆变器全球领先", "储能业务高增长", "海外营收占比高", "现金流健康"),
                        List.of("2025年储能系统出货量翻倍", "2024年逆变器全球市占率升至28%", "与沙特签署5GWh储能合作协议"),
                        "LOW",
                        "低风险"));

        put(
                new EnterpriseInfo(
                        "CUST-009",
                        "晶科能源股份有限公司",
                        "新能源",
                        "华东",
                        9.5,
                        3.8,
                        155.0,
                        32.0,
                        1.5,
                        List.of("组件出货量全球前二", "负债率偏高", "行业产能过剩压力", "毛利率持续下滑"),
                        List.of("2025年Q1组件价格同比下降35%", "2024年全年毛利率降至12%", "美国反规避调查导致出口受限"),
                        "WATCH",
                        "关注"));
    }

    private static void put(EnterpriseInfo info) {
        DATA.put(info.name(), info);
    }

    public static List<EnterpriseInfo> search(String keyword) {
        String kw = keyword != null ? keyword.trim() : "";
        if (kw.isEmpty()) return List.copyOf(DATA.values());
        return DATA.values().stream()
                .filter(
                        e ->
                                e.name().contains(kw)
                                        || e.industry().contains(kw)
                                        || e.region().contains(kw))
                .collect(Collectors.toList());
    }

    public static EnterpriseInfo get(String name) {
        return DATA.get(name);
    }

    public static List<EnterpriseInfo> all() {
        return List.copyOf(DATA.values());
    }
}
