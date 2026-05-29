package io.agentscope.examples.bankrisk.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MockEnterpriseData {

    public record EnterpriseInfo(
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
                        "XX农商银行",
                        "农村商业银行",
                        "华东",
                        7.2,
                        4.8,
                        135.0,
                        28.5,
                        -0.32,
                        List.of("资本充足率不足", "不良贷款率偏高", "拨备覆盖率接近红线", "盈利能力下降"),
                        List.of("2025Q1 监管通报指出资本缺口", "2024年核销不良贷款3.2亿元", "股东增资计划尚未落地"),
                        "HIGH",
                        "高风险"));

        put(
                new EnterpriseInfo(
                        "YY城商银行",
                        "城市商业银行",
                        "华南",
                        11.5,
                        1.8,
                        220.0,
                        42.1,
                        0.85,
                        List.of("经营稳健", "资产质量良好"),
                        List.of("2025Q1 获监管评级2A", "数字化转型项目完成验收"),
                        "LOW",
                        "低风险"));

        put(
                new EnterpriseInfo(
                        "ZZ村镇银行",
                        "村镇银行",
                        "华中",
                        9.1,
                        3.2,
                        165.0,
                        35.0,
                        0.15,
                        List.of("资本缓冲不足", "不良率上升趋势", "流动性管理待加强"),
                        List.of("2025年需重点关注不良贷款变化", "主要股东质押比例较高"),
                        "WATCH",
                        "关注"));

        put(
                new EnterpriseInfo(
                        "AA股份制银行",
                        "全国股份制商业银行",
                        "全国",
                        12.8,
                        1.2,
                        280.0,
                        55.3,
                        1.12,
                        List.of("资本充足", "资产质量优秀", "盈利能力强"),
                        List.of("2024年报获审计无保留意见", "成功发行200亿元永续债"),
                        "LOW",
                        "低风险"));

        put(
                new EnterpriseInfo(
                        "BB农商银行",
                        "农村商业银行",
                        "西南",
                        6.5,
                        5.9,
                        110.0,
                        22.0,
                        -0.78,
                        List.of("资本严重不足", "不良率远超监管红线", "拨备严重不足", "持续亏损"),
                        List.of("2024年监管强制要求整改", "2025Q1 不良率恶化至5.9%", "大额关联贷款出现违约"),
                        "CRITICAL",
                        "严重"));

        put(
                new EnterpriseInfo(
                        "CC城商银行",
                        "城市商业银行",
                        "华北",
                        10.2,
                        2.5,
                        185.0,
                        38.7,
                        0.42,
                        List.of("不良率需关注", "拨备覆盖率略低", "盈利能力中等"),
                        List.of("2025Q1 不良贷款小幅反弹", "互联网金融业务拓展中"),
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
